package hudson.plugins.emailext;

import org.junit.jupiter.api.Test;

public class MailAccountValidationTest {

    @Test
    public void testSetSmtpPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("25");
    }
}