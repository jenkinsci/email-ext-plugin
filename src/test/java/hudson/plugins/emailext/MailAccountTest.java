package hudson.plugins.emailext;

import static hudson.plugins.emailext.FormValidationMessageMatcher.hasMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
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
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class MailAccountTest {

    @Test
    @WithoutJenkins
    void testIsValidEmptyConfig() {
        JSONObject obj = new JSONObject();
        MailAccount account = new MailAccount(obj);
        assertFalse(account.isValid());
    }

    @Test
    @WithoutJenkins
    void testIsValidMissingAddress() {
        JSONObject obj = new JSONObject();
        obj.put("smtpHost", "mail.bar.com");
        obj.put("smtpPort", 25);
        MailAccount account = new MailAccount(obj);
        assertFalse(account.isValid());
    }

    @Test
    @WithoutJenkins
    void testIsValidNonAuthConfig() {
        JSONObject obj = new JSONObject();
        obj.put("address", "foo@bar.com");
        obj.put("smtpHost", "mail.bar.com");
        obj.put("smtpPort", 25);
        MailAccount account = new MailAccount(obj);
        assertTrue(account.isValid());
    }

    @Test
    void testIsValidAuthConfig(JenkinsRule j) {
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
    void testIsValidAuthConfigWithoutEncryption() {
        MailAccount account = new MailAccount();
        account.setAddress("joe@example.com");
        account.setCredentialsId("foo");
        assertTrue(account.isValid());
    }

    @Test
    @WithoutJenkins
    void testIsValidAuthConfigAndSSL() {
        MailAccount account = new MailAccount();
        account.setAddress("joe@example.com");
        account.setCredentialsId("foo");
        account.setUseSsl(true);
        assertTrue(account.isValid());
    }

    @Test
    @WithoutJenkins
    void testIsValidAuthConfigAndTLS() {
        MailAccount account = new MailAccount();
        account.setAddress("joe@example.com");
        account.setCredentialsId("foo");
        account.setUseTls(true);
        assertTrue(account.isValid());
    }

    @Test
    void testUpgradeDuringCredentialsGetter(JenkinsRule j) {
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
    void testFormValidationForInsecureAuth(JenkinsRule j) throws Exception {
        final String validCredentialId = "valid-id";
        SystemCredentialsProvider.getInstance()
                .getCredentials()
                .add(new UsernamePasswordCredentialsImpl(
                        CredentialsScope.GLOBAL, validCredentialId, "description", "username", "password"));

        MailAccountDescriptor mad = (MailAccountDescriptor) Jenkins.get().getDescriptor(MailAccount.class);

        assertThat(mad.doCheckCredentialsId(null, "", false, false), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, null, false, false), hasKind(Kind.OK));

        assertThat(mad.doCheckCredentialsId(null, null, true, false), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, null, false, true), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, null, true, true), hasKind(Kind.OK));

        assertThat(mad.doCheckCredentialsId(null, validCredentialId, true, false), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, validCredentialId, false, true), hasKind(Kind.OK));
        assertThat(mad.doCheckCredentialsId(null, validCredentialId, true, true), hasKind(Kind.OK));

        assertThat(
                mad.doCheckCredentialsId(null, validCredentialId, false, false),
                Matchers.allOf(
                        hasKind(Kind.WARNING),
                        hasMessage(
                                "For security when using authentication it is recommended to enable either TLS or SSL")));

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

    @Test
    @WithoutJenkins
    void testOAuth2FieldsDefaultNull() {
        MailAccount account = new MailAccount();
        assertNull(account.getTenantId());
        assertNull(account.getClientId());
        assertNull(account.getClientSecret());
    }

    @Test
    @WithoutJenkins
    void testSetAndGetTenantId() {
        MailAccount account = new MailAccount();
        account.setTenantId("test-tenant-id");
        assertEquals("test-tenant-id", account.getTenantId());
    }

    @Test
    @WithoutJenkins
    void testSetAndGetClientId() {
        MailAccount account = new MailAccount();
        account.setClientId("test-client-id");
        assertEquals("test-client-id", account.getClientId());
    }

    @Test
    @WithoutJenkins
    void testSetAndGetClientSecret() {
        MailAccount account = new MailAccount();
        account.setClientSecret("test-secret");
        assertEquals("test-secret", account.getClientSecret());
    }

    @Test
    @WithoutJenkins
    void testOAuth2FieldsTrimWhitespace() {
        MailAccount account = new MailAccount();
        account.setTenantId("  my-tenant  ");
        account.setClientId("  my-client  ");
        assertEquals("my-tenant", account.getTenantId());
        assertEquals("my-client", account.getClientId());
    }
}
