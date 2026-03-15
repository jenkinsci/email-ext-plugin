package hudson.plugins.emailext;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MailAccountValidationTest {

    @Test
    void testSetSmtpPort() {
        MailAccount account = new MailAccount();
        account.setSmtpPort("25");
        assertEquals("25", account.getSmtpPort());
    }
}