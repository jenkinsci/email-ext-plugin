package hudson.plugins.emailext;

import static hudson.plugins.emailext.FormValidationMessageMatcher.hasMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.jvnet.hudson.test.JenkinsMatchers.hasKind;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.plugins.emailext.MailAccount.MailAccountDescriptor;
import hudson.util.FormValidation.Kind;
import hudson.util.Secret;
import java.util.List;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

public class MailAccountTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @WithoutJenkins
    public void testIsValidEmptyConfig() {
        JSONObject obj = new JSONObject();
        MailAccount account = new MailAccount(obj);
        assertFalse(account.isValid());
    }

    @Test
    @WithoutJenkins
    public void testIsValidMissingAddress() {
        JSONObject obj = new JSONObject();
        obj.put("smtpHost", "mail.bar.com");
        obj.put("smtpPort", 25);
        MailAccount account = new MailAccount(obj);
        assertFalse(account.isValid());
    }

    @Test
    @WithoutJenkins
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
    @WithoutJenkins
    public void testIsValidAuthConfigWithoutEncryption() {
        MailAccount account = new MailAccount();
        account.setAddress("joe@example.com");
        account.setCredentialsId("foo");
        assertTrue(account.isValid());
    }

    @Test
    @WithoutJenkins
    public void testIsValidAuthConfigAndSSL() {
        MailAccount account = new MailAccount();
        account.setAddress("joe@example.com");
        account.setCredentialsId("foo");
        account.setUseSsl(true);
        assertTrue(account.isValid());
    }

    @Test
    @WithoutJenkins
    public void testIsValidAuthConfigAndTLS() {
        MailAccount account = new MailAccount();
        account.setAddress("joe@example.com");
        account.setCredentialsId("foo");
        account.setUseTls(true);
        assertTrue(account.isValid());
    }

    @Test
    public void testUpgradeDuringCredentialsGetter() {
        MailAccount account = new MailAccount();
        account.setSmtpUsername("foo");
        account.setSmtpPassword(Secret.fromString("bar"));

        assertNotNull(account.getCredentialsId());

        List<StandardUsernamePasswordCredentials> creds =
                CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class);
        assertEquals(1, creds.size());
        StandardUsernamePasswordCredentials c = creds.get(0);
        assertEquals("foo", c.getUsername());
        assertEquals("bar", c.getPassword().getPlainText());
    }

    @Test
    public void testFormValidationForInsecureAuth() throws Exception {
        final String validCredentialId = "valid-id";
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .add(new UsernamePasswordCredentialsImpl(
                        CredentialsScope.GLOBAL, validCredentialId, "description", "username", "password"));

        MailAccountDescriptor mad = (MailAccountDescriptor) Jenkins.get().getDescriptor(MailAccount.class);

        assertThat(mad.doCheckCredentialsId(null, "", false, false), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, null, false, false), hasKind(Kind.OK));

        // no auth but any combination of TLS/SSL is ok
        assertThat(mad.doCheckCredentialsId(null, null, true, false), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, null, false, true), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, null, true, true), hasKind(Kind.OK));

        // valid credentials with TLS
        assertThat(mad.doCheckCredentialsId(null, validCredentialId, true, false), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, validCredentialId, false, true), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, validCredentialId, true, true), hasKind(Kind.OK));

        // valid credentials without TLS produce a warning (error in FIPS, but requires system property)
        assertThat(
                mad.doCheckCredentialsId(null, validCredentialId, false, false),
                Matchers.allOf(
                        hasKind(Kind.WARNING),
                        hasMessage(
                                "For security when using authentication it is recommended to enable either TLS or SSL")));

        // non-valid creds show the error regardless of SSL/TLS
        assertThat(
                mad.doCheckCredentialsId(null, "bogus", false, false),
                Matchers.allOf(
                        hasKind(Kind.ERROR),
                        hasMessage(
                                containsString(
                                        "For security when using authentication it is recommended to enable either TLS or SSL")),
                        hasMessage(containsString("Cannot find currently selected credentials"))));
        assertThat(
                mad.doCheckCredentialsId(null, "bogus", true, false),
                Matchers.allOf(hasKind(Kind.ERROR), hasMessage("Cannot find currently selected credentials")));
        assertThat(
                mad.doCheckCredentialsId(null, "bogus", false, true),
                Matchers.allOf(hasKind(Kind.ERROR), hasMessage("Cannot find currently selected credentials")));
        assertThat(
                mad.doCheckCredentialsId(null, "bogus", true, true),
                Matchers.allOf(hasKind(Kind.ERROR), hasMessage("Cannot find currently selected credentials")));
    }
}
