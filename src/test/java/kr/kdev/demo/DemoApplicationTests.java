package kr.kdev.demo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
    void givenTo_whenSend_thenSuccess() {
        int max = 25;
        EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
                .from(from)
                .withSubject("Test email")
                .withPlainText("This is test mail for batch performance");
        CountDownLatch latch = new CountDownLatch(max);
        IntStream.rangeClosed(1, max)
                .parallel().forEach(i -> {
                    Email email = builder.to(toFormat.formatted(i)).buildEmail();
                    mailer.sendMail(email, true);
                    try {
                        Thread.sleep(Duration.ofSeconds(5L).toMillis());
                        latch.countDown();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });

        Assertions.assertDoesNotThrow(() -> latch.await());
        mailer.shutdownConnectionPool();
    }

}
