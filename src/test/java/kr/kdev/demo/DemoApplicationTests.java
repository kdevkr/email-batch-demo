package kr.kdev.demo;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.simplejavamail.MailException;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DemoApplicationTests {

    @Autowired
    Mailer mailer;
    @Value("${spring.mail.from}")
    String from;
    @Value("${test.mail.to-format}")
    String toFormat;

    @Test
    void contextLoads() {
        Assertions.assertNotNull(mailer);
        mailer.testConnection();
    }

    @Test
    void givenTooManyRecipients_whenSendMail_thenThrowErrors() {
        String[] addresses = IntStream.rangeClosed(1, 51)
                .mapToObj(i -> toFormat.formatted(i))
                .toList()
                .toArray(String[]::new);

        Email email = EmailBuilder.startingBlank()
                .from(from)
                .toMultiple(addresses)
                .withSubject("Test!!!")
                .withPlainText("This is test mail")
                .buildEmail();

        // NOTE: Caused by: org.eclipse.angus.mail.smtp.SMTPSendFailedException: 554 Transaction failed: Recipient count exceeds 50.
        MailException exception = Assertions.assertThrows(MailException.class, () -> mailer.sendMail(email));
        exception.printStackTrace();
    }

    @Test
    void givenTo_whenSend_thenSuccess() {
        int max = 100;
        RateLimiter rateLimiter = RateLimiter.create(10.0); // NOTE: sending limits

        CountDownLatch latch = new CountDownLatch(max);
        IntStream.rangeClosed(1, max)
                .parallel().forEach(i -> {
                    rateLimiter.acquire();
                    EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
                            .from(from)
                            .withSubject("Test email")
                            .withPlainText("This is test mail for batch performance");
                    Email email = builder.clearOverrideReceivers()
                            .to(toFormat.formatted(i)).buildEmail();
                    mailer.sendMail(email, true);
                    System.out.println("Sent email to " + i);
                    latch.countDown();
                });

        Assertions.assertDoesNotThrow(() -> {
            latch.await();
            Thread.sleep(Duration.ofSeconds(5).toMillis());
        });
        mailer.shutdownConnectionPool();
    }

}
