package hudson.plugins.emailext;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MailAccountValidationTest {

    @Test
    public void testSetSmtpPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("25");
        String result = account.getSmtpPort();
        assertEquals("25", result);
    }
}