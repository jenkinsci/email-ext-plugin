package hudson.plugins.emailext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.ExtensionList;
import hudson.model.FreeStyleBuild;
import hudson.plugins.emailext.plugins.trigger.AbortedTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedTrigger;
import hudson.plugins.emailext.plugins.trigger.RegressionTrigger;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@WithJenkinsConfiguredWithCode
class ExtendedEmailPublisherDescriptorJCasCTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void shouldValidatedJCasCConfiguration(JenkinsConfiguredWithCodeRule r) {
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
        assertEquals(
                "hudson.plugins.emailext.plugins.trigger.FailureTrigger",
                descriptor.getDefaultTriggerIds().get(0));

        assertEquals(
                1,
                descriptor.getDefaultAttachBuildLog(),
                "JCasC should configure defaultAttachBuildLog to 1 (Attach Build Log)");
        assertTrue(descriptor.isDebugMode());
    }

    @Test
    @ConfiguredWithCode("configuration-as-code-with-triggers.yml")
    void shouldBeAbleToConfigureTriggers(JenkinsConfiguredWithCodeRule r) {
        final ExtendedEmailPublisherDescriptor descriptor =
                ExtensionList.lookupSingleton(ExtendedEmailPublisherDescriptor.class);
        assertNotNull(descriptor);

        final List<String> expectedTriggers = Arrays.asList(
                RegressionTrigger.class.getName(), AbortedTrigger.class.getName(), FixedTrigger.class.getName());

        assertThat(
                descriptor.getDefaultTriggerIds(),
                Matchers.contains(
                        expectedTriggers.stream().map(Matchers::equalTo).collect(Collectors.toList())));
    }

    @Test
    @ConfiguredWithCode("configuration-as-code-upgrade.yml")
    void shouldValidatedJCasCConfigurationWithUpgrade(JenkinsConfiguredWithCodeRule r) {
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
        List<Credentials> creds =
                CredentialsProvider.lookupCredentials(com.cloudbees.plugins.credentials.Credentials.class);
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
        assertEquals(
                "hudson.plugins.emailext.plugins.trigger.FailureTrigger",
                descriptor.getDefaultTriggerIds().get(0));

        assertTrue(descriptor.isDebugMode());
    }

    @Test
    @ConfiguredWithCode("configuration-as-code-upgrade-creds-check.yml")
    void shouldUseCorrectCredentialsAfterUpgrade(JenkinsConfiguredWithCodeRule r) throws Exception {
        final ExtendedEmailPublisherDescriptor descriptor =
                ExtensionList.lookupSingleton(ExtendedEmailPublisherDescriptor.class);

        FreeStyleBuild build = Mockito.mock(FreeStyleBuild.class);

        Authenticator authenticator = descriptor.getAuthenticatorProvider().apply(descriptor.getMailAccount(), build);
        Method method = authenticator.getClass().getDeclaredMethod("getPasswordAuthentication");
        PasswordAuthentication passwordAuthentication = (PasswordAuthentication) method.invoke(authenticator);
        assertNotNull(passwordAuthentication);
        assertEquals("smtp-username-xyz", passwordAuthentication.getUserName());
        assertEquals("smtp-password-xyz", passwordAuthentication.getPassword());
    }

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void mailAccountShouldPersistAfterMultipleAccesses(JenkinsConfiguredWithCodeRule r) {
        final ExtendedEmailPublisherDescriptor descriptor =
                ExtensionList.lookupSingleton(ExtendedEmailPublisherDescriptor.class);

        MailAccount account = descriptor.getMailAccount();
        assertNotNull(account, "MailAccount should not be null");
        assertTrue(account.isDefaultAccount());
        assertEquals("smtp-host-xyz", account.getSmtpHost());
        assertEquals("9876", account.getSmtpPort());
        assertEquals("smtp-credentials-xyz", account.getCredentialsId());
        assertEquals("adv-properties-xyz", account.getAdvProperties());
        assertTrue(account.isUseSsl());
        assertTrue(account.isUseTls());

        for (int i = 0; i < 3; i++) {
            account = descriptor.getMailAccount();
            assertNotNull(account, "MailAccount should not be null on access " + (i + 2));
            assertEquals("smtp-host-xyz", account.getSmtpHost(), "smtpHost should persist on access " + (i + 2));
            assertEquals("9876", account.getSmtpPort(), "smtpPort should persist on access " + (i + 2));
            assertEquals(
                    "smtp-credentials-xyz",
                    account.getCredentialsId(),
                    "credentialsId should persist on access " + (i + 2));
            assertEquals(
                    "adv-properties-xyz",
                    account.getAdvProperties(),
                    "advProperties should persist on access " + (i + 2));
            assertTrue(account.isUseSsl(), "useSsl should persist on access " + (i + 2));
            assertTrue(account.isUseTls(), "useTls should persist on access " + (i + 2));
        }
    }

    @Test
    @ConfiguredWithCode("configuration-as-code-with-templates.yml")
    void shouldProvisionEmailTemplates(JenkinsConfiguredWithCodeRule r) throws Exception {
        final ExtendedEmailPublisherDescriptor descriptor =
                ExtensionList.lookupSingleton(ExtendedEmailPublisherDescriptor.class);
        assertNotNull(descriptor);

        // Verify templates are stored in the descriptor
        List<EmailTemplate> templates = descriptor.getEmailTemplates();
        assertEquals(2, templates.size());
        assertEquals("test-template.groovy", templates.get(0).getName());
        assertEquals("test-jelly-template.jelly", templates.get(1).getName());

        // Verify template files were written to disk
        File templatesDir = new File(r.jenkins.getRootDir(), "email-templates");
        assertTrue(templatesDir.exists(), "email-templates directory should exist");

        File groovyTemplate = new File(templatesDir, "test-template.groovy");
        assertTrue(groovyTemplate.exists(), "Groovy template file should exist");
        String groovyContent = Files.readString(groovyTemplate.toPath(), StandardCharsets.UTF_8);
        assertTrue(
                groovyContent.contains("CasC provisioned template"), "Groovy template should contain expected content");

        File jellyTemplate = new File(templatesDir, "test-jelly-template.jelly");
        assertTrue(jellyTemplate.exists(), "Jelly template file should exist");
        String jellyContent = Files.readString(jellyTemplate.toPath(), StandardCharsets.UTF_8);
        assertTrue(
                jellyContent.contains("CasC provisioned jelly template"),
                "Jelly template should contain expected content");
    }

    @Test
    void shouldRejectInvalidTemplateNames() {
        // Path traversal
        assertThrows(IllegalArgumentException.class, () -> new EmailTemplate("../../etc/passwd.groovy", "content"));

        // Directory separators
        assertThrows(IllegalArgumentException.class, () -> new EmailTemplate("subdir/template.groovy", "content"));
        assertThrows(IllegalArgumentException.class, () -> new EmailTemplate("subdir\\template.groovy", "content"));

        // Bad extensions
        assertThrows(IllegalArgumentException.class, () -> new EmailTemplate("script.sh", "content"));
        assertThrows(IllegalArgumentException.class, () -> new EmailTemplate("script.py", "content"));
        assertThrows(IllegalArgumentException.class, () -> new EmailTemplate("noextension", "content"));

        // Empty and whitespace
        assertThrows(IllegalArgumentException.class, () -> new EmailTemplate("", "content"));
        assertThrows(IllegalArgumentException.class, () -> new EmailTemplate("   ", "content"));

        // Valid names should not throw
        assertDoesNotThrow(() -> new EmailTemplate("valid-template.groovy", "content"));
        assertDoesNotThrow(() -> new EmailTemplate("my_template.jelly", "content"));
        assertDoesNotThrow(() -> new EmailTemplate("report.template", "content"));
    }
}
