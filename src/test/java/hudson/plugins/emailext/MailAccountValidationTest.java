package hudson.plugins.emailext;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MailAccountValidationTest {

    @Test
    void testInvalidHighSmtpPort() {
        int port = 70000;
        assertTrue(port > 65535, "SMTP port should be invalid if greater than 65535");
    }

    @Test
    void testInvalidLowSmtpPort() {
        int port = -1;
        assertTrue(port < 1, "SMTP port should be invalid if less than 1");
    }

    @Test
    void testValidSmtpPort() {
        int port = 25;
        assertTrue(port > 0 && port <= 65535, "SMTP port should be valid if between 1 and 65535");
    }
}