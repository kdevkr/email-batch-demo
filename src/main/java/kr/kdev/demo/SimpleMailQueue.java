package kr.kdev.demo;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.MailException;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleMailQueue {
    private static final int MAX_RETRY_COUNT = 3;
    private final Mailer mailer;
    private final BlockingQueue<Email> queue = Queues.newLinkedBlockingDeque();
    private final Map<Integer, Integer> retryCount = Maps.newConcurrentMap();
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

    public void produce(Email email) {
        queue.add(email);
    }

    @Scheduled(fixedRateString = "PT1S")
    public void consume() {
        if (queue.isEmpty()) {
            return;
        }

        List<Email> emails = new LinkedList<>();
        queue.drainTo(emails, 100);

        emails.stream().parallel().forEach(email -> {
            if (rateLimiter.tryAcquire()) {
                try {
                    mailer.sendMail(email);
                    log.info("Message-ID: {}", email.getId());
                } catch (MailException e) {
                    log.error("Failed to send email: {}", e.getMessage());
                    handleEmailSendFailure(email);
                }
            } else {
                queue.add(email);
            }
        });
    }

    private void handleEmailSendFailure(Email email) {
        int emailHash = email.hashCode();
        int currentRetryCount = retryCount.getOrDefault(emailHash, 0) + 1;
        if (currentRetryCount > MAX_RETRY_COUNT) {
            retryCount.remove(emailHash);
            return;
        }

        log.info("Retry send email {}/{}", currentRetryCount, MAX_RETRY_COUNT);
        retryCount.put(emailHash, currentRetryCount);
        queue.add(email);
    }
}
