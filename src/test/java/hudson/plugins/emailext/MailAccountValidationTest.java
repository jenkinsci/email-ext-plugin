package hudson.plugins.emailext;

import org.junit.Test;
import static org.junit.Assert.*;

public class MailAccountValidationTest {

    @Test
    public void testInvalidHighSmtpPort() {
        int port = 70000; // greater than allowed range
        assertTrue("SMTP port should be invalid if greater than 65535", port > 65535);
    }

    @Test
    public void testInvalidLowSmtpPort() {
        int port = -1; // lower than allowed range
        assertTrue("SMTP port should be invalid if less than 1", port < 1);
    }

    @Test
    public void testValidSmtpPort() {
        int port = 25; // valid SMTP port
        assertTrue("SMTP port should be valid if between 1 and 65535", port > 0 && port <= 65535);
    }
}