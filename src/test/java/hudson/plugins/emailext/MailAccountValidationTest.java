package hudson.plugins.emailext;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MailAccountValidationTest {

    @Test
    public void testSetSmtpPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("25");
        assertEquals("25", account.getSmtpPort());
    }

    @Test
    public void testInvalidSmtpPortTooLow() {
        MailAccount account = new MailAccount();
        assertThrows(IllegalArgumentException.class, () -> {
            account.setSmtpPort("0");
        });
    }

    @Test
    public void testInvalidSmtpPortTooHigh() {
        MailAccount account = new MailAccount();
        assertThrows(IllegalArgumentException.class, () -> {
            account.setSmtpPort("70000");
        });
    }
}