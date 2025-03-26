package kr.kdev.demo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.simplejavamail.api.mailer.Mailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DemoApplicationTests {

    @Autowired
    Mailer mailer;

    @Test
    void contextLoads() {
        Assertions.assertNotNull(mailer);
        mailer.testConnection();
    }

}
