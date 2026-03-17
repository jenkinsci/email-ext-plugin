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
    void testAnotherValidPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("587");
        assertEquals("587", account.getSmtpPort());
    }
}