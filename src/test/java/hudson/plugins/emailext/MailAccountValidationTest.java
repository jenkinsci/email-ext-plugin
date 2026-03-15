package hudson.plugins.emailext;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MailAccountValidationTest {

    @Test
    void testInvalidHighSmtpPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("70000");

        assertEquals("70000", account.getSmtpPort());
    }

    @Test
    void testInvalidLowSmtpPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("-1");

        assertEquals("-1", account.getSmtpPort());
    }

    @Test
    void testValidSmtpPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("25");

        assertEquals("25", account.getSmtpPort());
    }
}