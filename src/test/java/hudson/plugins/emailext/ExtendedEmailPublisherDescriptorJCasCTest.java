package hudson.plugins.emailext;

import hudson.ExtensionList;
import hudson.plugins.emailext.plugins.trigger.AbortedTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedTrigger;
import hudson.plugins.emailext.plugins.trigger.RegressionTrigger;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ExtendedEmailPublisherDescriptorJCasCTest {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void shouldValidatedJCasCConfiguration() throws Exception {
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
        assertEquals("smtp-username", account.getSmtpUsername());
        assertEquals("smtp-password", account.getSmtpPassword().getPlainText());
        assertTrue(account.isUseSsl());
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
        assertEquals("smtp-username-xyz", account.getSmtpUsername());
        assertEquals("smtp-password-xyz", account.getSmtpPassword().getPlainText());
        assertEquals("adv-properties-xyz", account.getAdvProperties());
        assertTrue(account.isUseSsl());

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
    public void shouldBeAbleToConfigureTriggers() throws Exception {
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
}
