package hudson.plugins.emailext;

import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MailAccountTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test @WithoutJenkins
    public void testIsValidEmptyConfig() {
        JSONObject obj = new JSONObject();
        MailAccount account = new MailAccount(obj);
        assertFalse(account.isValid());
    }

    @Test @WithoutJenkins
    public void testIsValidMissingAddress() {
        JSONObject obj = new JSONObject();
        obj.put("smtpHost", "mail.bar.com");
        obj.put("smtpPort", 25);
        MailAccount account = new MailAccount(obj);
        assertFalse(account.isValid());
    }

    @Test @WithoutJenkins
    public void testIsValidNonAuthConfig() {
        JSONObject obj = new JSONObject();
        obj.put("address", "foo@bar.com");
        obj.put("smtpHost", "mail.bar.com");
        obj.put("smtpPort", 25);
        MailAccount account = new MailAccount(obj);
        assertTrue(account.isValid());
    }

    @Test @WithoutJenkins
    public void testIsValidAuthConfigMissingPassword() {
        JSONObject obj = new JSONObject();
        obj.put("address", "foo@bar.com");
        obj.put("smtpHost", "mail.bar.com");
        obj.put("smtpPort", 25);
        obj.put("auth", true);
        obj.put("smtpUsername", "foo");

        MailAccount account = new MailAccount(obj);
        assertFalse(account.isValid());
    }

    @Test
    public void testIsValidAuthConfig() {
        JSONObject obj = new JSONObject();
        obj.put("address", "foo@bar.com");
        obj.put("smtpHost", "mail.bar.com");
        obj.put("smtpPort", 25);
        obj.put("auth", true);
        obj.put("smtpUsername", "foo");
        obj.put("smtpPassword", "bar");

        MailAccount account = new MailAccount(obj);
        assertTrue(account.isValid());
    }
}
