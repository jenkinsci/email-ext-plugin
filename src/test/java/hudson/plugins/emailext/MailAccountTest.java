package hudson.plugins.emailext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import java.util.List;

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

    @Test
    public void testIsValidAuthConfig() {
        JSONObject obj = new JSONObject();
        obj.put("address", "foo@bar.com");
        obj.put("smtpHost", "mail.bar.com");
        obj.put("smtpPort", 25);
        obj.put("auth", true);
        obj.put("credentialsId", "foo");

        MailAccount account = new MailAccount(obj);
        assertTrue(account.isValid());
    }

    @Test
    public void testUpgradeDuringCredentialsGetter() {
        MailAccount account = new MailAccount();
        account.setSmtpUsername("foo");
        account.setSmtpPassword(Secret.fromString("bar"));

        assertNotNull(account.getCredentialsId());

        List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class);
        assertEquals(1, creds.size());
        StandardUsernamePasswordCredentials c = creds.get(0);
        assertEquals("foo", c.getUsername());
        assertEquals("bar", c.getPassword().getPlainText());
    }
}
