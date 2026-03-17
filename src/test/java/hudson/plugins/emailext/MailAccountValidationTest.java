package hudson.plugins.emailext;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MailAccountValidationTest {

    @Test
    void testValidSmtpPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("25");
        assertEquals("25", account.getSmtpPort());
    }

    @Test
    void testInvalidHighPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("70000");
        assertEquals("70000", account.getSmtpPort());
    }

    @Test
    void testInvalidLowPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("-1");
        assertEquals("-1", account.getSmtpPort());
    }
}