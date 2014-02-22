package hudson.plugins.emailext;
import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

public class EmailTypeTest {
    
    @Rule
    public static final JenkinsRule j = new JenkinsRule();

    @Test
    public void testHasNoRecipients() {
        EmailType t = new EmailType();

        assertFalse(t.getHasRecipients());
    }

    @Test
    public void testHasDeveloperRecipients() {
        EmailType t = new EmailType();
        
        t.addRecipientProvider(new DevelopersRecipientProvider());
        
        assertTrue(t.getHasRecipients());
    }

    @Test
    public void testHasRecipientList() {
        EmailType t = new EmailType();
        
        t.addRecipientProvider(new ListRecipientProvider());
        
        assertTrue(t.getHasRecipients());
    }

    @Test
    public void testHasDeveloperAndRecipientList() {
        EmailType t = new EmailType();

        t.addRecipientProvider(new ListRecipientProvider());
        t.addRecipientProvider(new DevelopersRecipientProvider());

        assertTrue(t.getHasRecipients());
    }

    @Test
    public void testCompressBuildAttachment() {
        EmailType t = new EmailType();
        t.setCompressBuildLog(true);

        assertTrue(t.getCompressBuildLog());
    }

    @Test
    public void testDefaultCompressBuildAttachment() {
        EmailType t = new EmailType();

        assertFalse(t.getCompressBuildLog());
    }
    
    @Test
    public void testUpgadeToRecipientProvider() throws IOException {
        URL url = this.getClass().getResource("/recipient-provider-upgrade.xml");
        File jobConfig = new File(url.getFile());    

        final ExtendedEmailPublisherDescriptor desc = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        FreeStyleProject prj = j.createFreeStyleProject();
        prj.updateByXml((Source)new StreamSource(new FileReader(jobConfig)));
        
        ExtendedEmailPublisher pub = (ExtendedEmailPublisher)prj.getPublisher(desc);
        
        // make sure the publisher got picked up
        assertNotNull(pub);
        
        // make sure the trigger was marshalled
        assertFalse(0 == pub.configuredTriggers.size());
        
        // should have developers, requestor and culprits
        assertEquals(3, pub.configuredTriggers.get(0).getEmail().getRecipientProviders().size());
    }
}
