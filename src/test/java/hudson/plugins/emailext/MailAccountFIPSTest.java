package hudson.plugins.emailext;

import static hudson.plugins.emailext.FormValidationMessageMatcher.hasMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.jvnet.hudson.test.JenkinsMatchers.hasKind;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.plugins.emailext.MailAccount.MailAccountDescriptor;
import hudson.util.FormValidation.Kind;
import jenkins.model.Jenkins;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FlagRule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

public class MailAccountFIPSTest {

    @ClassRule
    public static FlagRule<String> fipsSystemPropertyRule =
            FlagRule.systemProperty("jenkins.security.FIPS140.COMPLIANCE", "true");

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @WithoutJenkins
    public void testIsValidNonAuthConfig() {
        MailAccount account = new MailAccount();
        account.setAddress("joe@example.com");
        assertTrue(account.isValid());
    }

    @Test
    @WithoutJenkins
    public void testIsNotValidAuthConfigWithoutEncryption() {
        MailAccount account = new MailAccount();
        account.setAddress("joe@example.com");
        account.setCredentialsId("foo");
        assertFalse(account.isValid());
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
    public void testFormValidationForInsecureAuth() {
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

        // valid credentials without TLS produce a error
        assertThat(
                mad.doCheckCredentialsId(null, validCredentialId, false, false),
                allOf(hasKind(Kind.ERROR), hasMessage("Authentication requires either TLS or SSL to be enabled")));

        // non-valid creds show the error regardless of SSL/TLS
        assertThat(
                mad.doCheckCredentialsId(null, "bogus", false, false),
                allOf(
                        hasKind(Kind.ERROR),
                        hasMessage(containsString("Authentication requires either TLS or SSL to be enabled")),
                        hasMessage(containsString("Cannot find currently selected credentials"))));
        assertThat(
                mad.doCheckCredentialsId(null, "bogus", true, false),
                allOf(hasKind(Kind.ERROR), hasMessage("Cannot find currently selected credentials")));
        assertThat(
                mad.doCheckCredentialsId(null, "bogus", false, true),
                allOf(hasKind(Kind.ERROR), hasMessage("Cannot find currently selected credentials")));
        assertThat(
                mad.doCheckCredentialsId(null, "bogus", true, true),
                allOf(hasKind(Kind.ERROR), hasMessage("Cannot find currently selected credentials")));
    }
}
