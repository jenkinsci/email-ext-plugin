package hudson.plugins.emailext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.ExtensionList;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.plugins.trigger.AbortedTrigger;
import hudson.plugins.emailext.plugins.trigger.AlwaysTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedTrigger;
import hudson.plugins.emailext.plugins.trigger.RegressionTrigger;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;

import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mockito;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class ExtendedEmailPublisherDescriptorJCasCTest {

    @Rule public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();
    @Rule public TestName testName = new TestName();

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void shouldValidatedJCasCConfiguration() {
        final ExtendedEmailPublisherDescriptor descriptor =
                ExtensionList.lookupSingleton(ExtendedEmailPublisherDescriptor.class);
        assertNotNull(descriptor);

        assertEquals("@domain.extension", descriptor.getDefaultSuffix());

        assertEquals(1, descriptor.getAddAccounts().size());
        MailAccount account = descriptor.getAddAccounts().get(0);
        assertFalse(account.isDefaultAccount());
        assertEquals("foo.bar", account.getAddress());
        assertEquals("smtp-host", account.getSmtpHost());
        assertEquals("1234", account.getSmtpPort());
        assertEquals("credentials-id", account.getCredentialsId());
        assertTrue(account.isUseSsl());
        assertTrue(account.isUseTls());
        assertEquals("avd-properties", account.getAdvProperties());

        assertEquals("UTF-8", descriptor.getCharset());
        assertEquals("text/html", descriptor.getDefaultContentType());
        assertEquals("DEFAULT EMAIL SUBJECT", descriptor.getDefaultSubject());
        assertEquals("DEFAULT BODY", descriptor.getDefaultBody());
        assertEquals("emergency-reroute", descriptor.getEmergencyReroute());
        assertEquals(42, descriptor.getMaxAttachmentSizeMb());

        account = descriptor.getMailAccount();
        assertTrue(account.isDefaultAccount());
        assertEquals("smtp-host-xyz", account.getSmtpHost());
        assertEquals("9876", account.getSmtpPort());
        assertEquals("smtp-credentials-xyz", account.getCredentialsId());
        assertEquals("adv-properties-xyz", account.getAdvProperties());
        assertTrue(account.isUseSsl());
        assertTrue(account.isUseTls());

        assertEquals("first-account@domain.extension", descriptor.getDefaultRecipients());
        assertEquals("@domain.extension", descriptor.getAllowedDomains());
        assertEquals("not-this-committer", descriptor.getExcludedCommitters());
        assertEquals("list-id", descriptor.getListId());
        assertTrue(descriptor.getPrecedenceBulk());
        assertEquals("no-reply@domain.extension", descriptor.getDefaultReplyTo());
        assertTrue(descriptor.isAdminRequiredForTemplateTesting());
        assertTrue(descriptor.isWatchingEnabled());
        assertTrue(descriptor.isAllowUnregisteredEnabled());
        assertEquals("default-presend-script", descriptor.getDefaultPresendScript());
        assertEquals("defaultpostsend-script", descriptor.getDefaultPostsendScript());

        assertEquals(0, descriptor.getDefaultClasspath().size());
        assertEquals(1, descriptor.getDefaultTriggerIds().size());
        assertEquals("hudson.plugins.emailext.plugins.trigger.FailureTrigger", descriptor.getDefaultTriggerIds().get(0));

        assertTrue(descriptor.isDebugMode());
    }

    @Test
    @ConfiguredWithCode("configuration-as-code-with-triggers.yml")
    public void shouldBeAbleToConfigureTriggers() {
        final ExtendedEmailPublisherDescriptor descriptor =
              ExtensionList.lookupSingleton(ExtendedEmailPublisherDescriptor.class);
        assertNotNull(descriptor);

        final List<String> expectedTriggers = Arrays.asList(
              RegressionTrigger.class.getName(),
              AbortedTrigger.class.getName(),
              FixedTrigger.class.getName()
        );

        MatcherAssert.assertThat(
              descriptor.getDefaultTriggerIds(),
              Matchers.contains(expectedTriggers.stream().map(Matchers::equalTo).collect(Collectors.toList()))
        );
    }

    @Test
    @ConfiguredWithCode("configuration-as-code-upgrade.yml")
    public void shouldValidatedJCasCConfigurationWithUpgrade() {
        final ExtendedEmailPublisherDescriptor descriptor =
                ExtensionList.lookupSingleton(ExtendedEmailPublisherDescriptor.class);
        assertNotNull(descriptor);

        assertEquals("@domain.extension", descriptor.getDefaultSuffix());

        assertEquals(1, descriptor.getAddAccounts().size());
        MailAccount account = descriptor.getAddAccounts().get(0);
        assertFalse(account.isDefaultAccount());
        assertEquals("foo.bar", account.getAddress());
        assertEquals("smtp-host", account.getSmtpHost());
        assertEquals("1234", account.getSmtpPort());
        assertNotNull(account.getCredentialsId());
        assertNull(account.getSmtpUsername());
        assertNull(account.getSmtpPassword());
        assertTrue(account.isUseSsl());
        assertTrue(account.isUseTls());
        assertEquals("avd-properties", account.getAdvProperties());

        assertEquals("UTF-8", descriptor.getCharset());
        assertEquals("text/html", descriptor.getDefaultContentType());
        assertEquals("DEFAULT EMAIL SUBJECT", descriptor.getDefaultSubject());
        assertEquals("DEFAULT BODY", descriptor.getDefaultBody());
        assertEquals("emergency-reroute", descriptor.getEmergencyReroute());
        assertEquals(42, descriptor.getMaxAttachmentSizeMb());

        account = descriptor.getMailAccount();
        assertTrue(account.isDefaultAccount());
        assertEquals("smtp-host-xyz", account.getSmtpHost());
        assertEquals("9876", account.getSmtpPort());
        assertNotNull(account.getCredentialsId());
        assertNull(account.getSmtpUsername());
        assertNull(account.getSmtpPassword());
        assertEquals("adv-properties-xyz", account.getAdvProperties());
        assertTrue(account.isUseSsl());
        assertTrue(account.isUseTls());

        // check that credentials were created for the two accounts
        List<Credentials> creds = CredentialsProvider.lookupCredentials(com.cloudbees.plugins.credentials.Credentials.class);
        assertEquals(2, creds.size());
        for (Credentials c : creds) {
            assertEquals(UsernamePasswordCredentialsImpl.class, c.getClass());
        }

        assertEquals("first-account@domain.extension", descriptor.getDefaultRecipients());
        assertEquals("@domain.extension", descriptor.getAllowedDomains());
        assertEquals("not-this-committer", descriptor.getExcludedCommitters());
        assertEquals("list-id", descriptor.getListId());
        assertTrue(descriptor.getPrecedenceBulk());
        assertEquals("no-reply@domain.extension", descriptor.getDefaultReplyTo());
        assertTrue(descriptor.isAdminRequiredForTemplateTesting());
        assertTrue(descriptor.isWatchingEnabled());
        assertTrue(descriptor.isAllowUnregisteredEnabled());
        assertEquals("default-presend-script", descriptor.getDefaultPresendScript());
        assertEquals("defaultpostsend-script", descriptor.getDefaultPostsendScript());

        assertEquals(0, descriptor.getDefaultClasspath().size());
        assertEquals(1, descriptor.getDefaultTriggerIds().size());
        assertEquals("hudson.plugins.emailext.plugins.trigger.FailureTrigger", descriptor.getDefaultTriggerIds().get(0));

        assertTrue(descriptor.isDebugMode());
    }

    @Test
    @ConfiguredWithCode("configuration-as-code-upgrade-creds-check.yml")
    public void shouldUseCorrectCredentialsAfterUpgrade() throws Exception {
        final ExtendedEmailPublisherDescriptor descriptor =
                ExtensionList.lookupSingleton(ExtendedEmailPublisherDescriptor.class);

        FreeStyleBuild build = Mockito.mock(FreeStyleBuild.class);

        Authenticator authenticator = descriptor.getAuthenticatorProvider().apply(descriptor.getMailAccount(), build);
        Method method = authenticator.getClass().getDeclaredMethod("getPasswordAuthentication");
        PasswordAuthentication passwordAuthentication = (PasswordAuthentication)method.invoke(authenticator);
        assertNotNull(passwordAuthentication);
        assertEquals("smtp-username-xyz", passwordAuthentication.getUserName());
        assertEquals("smtp-password-xyz", passwordAuthentication.getPassword());
    }
}
