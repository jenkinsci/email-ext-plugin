package hudson.plugins.emailext;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MailAccountValidationTest {

    @Test
    public void testInvalidPortTooLow() {
        MailAccount account = new MailAccount();
        assertThrows(IllegalArgumentException.class, () -> {
            account.setSmtpPort("0");
        });
    }

    @Test
    public void testInvalidPortTooHigh() {
        MailAccount account = new MailAccount();
        assertThrows(IllegalArgumentException.class, () -> {
            account.setSmtpPort("70000");
        });
    }

    @Test
    public void testValidPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("25");
    }
}