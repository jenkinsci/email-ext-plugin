package hudson.plugins.emailext;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ExtendedEmailPublisherDescriptorJCasCTest {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void shouldValidatedJCasCConfiguration() throws Exception {
        final ExtendedEmailPublisherDescriptor extendedEmailPublisherDescriptor =
              ExtensionList.lookupSingleton(ExtendedEmailPublisherDescriptor.class);
        assertNotNull(extendedEmailPublisherDescriptor);

        assertEquals("@domain.extension", extendedEmailPublisherDescriptor.getDefaultSuffix());
        // TODO addAccounts
        assertEquals("UTF-8", extendedEmailPublisherDescriptor.getCharset());
        assertEquals("text/html", extendedEmailPublisherDescriptor.getDefaultContentType());
        assertEquals("DEFAULT EMAIL SUBJECT", extendedEmailPublisherDescriptor.getDefaultSubject());
        assertEquals("DEFAULT BODY", extendedEmailPublisherDescriptor.getDefaultBody());
        assertEquals("emergency-reroute", extendedEmailPublisherDescriptor.getEmergencyReroute());
        assertEquals(42, extendedEmailPublisherDescriptor.getMaxAttachmentSize());
        // TODO mailAccount
        assertEquals("first-account@domain.extension", extendedEmailPublisherDescriptor.getDefaultRecipients());
        assertEquals("@domain.extension", extendedEmailPublisherDescriptor.getAllowedDomains());
        assertEquals("not-this-committer", extendedEmailPublisherDescriptor.getExcludedCommitters());
        assertEquals("list-id", extendedEmailPublisherDescriptor.getListId());
        assertTrue(extendedEmailPublisherDescriptor.getPrecedenceBulk());
        assertEquals("no-reply@domain.extension", extendedEmailPublisherDescriptor.getDefaultReplyTo());
        assertTrue(extendedEmailPublisherDescriptor.isAdminRequiredForTemplateTesting());
        assertTrue(extendedEmailPublisherDescriptor.isWatchingEnabled());
        assertTrue(extendedEmailPublisherDescriptor.isAllowUnregisteredEnabled());
        assertEquals("default-presend-script", extendedEmailPublisherDescriptor.getDefaultPresendScript());
        assertEquals("defaultpostsend-script", extendedEmailPublisherDescriptor.getDefaultPostsendScript());
        // TODO defaultClasspath
        // TODO defaultTriggerIds
        assertTrue(extendedEmailPublisherDescriptor.isDebugMode());
    }
}
