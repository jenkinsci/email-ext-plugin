package hudson.plugins.emailext;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import org.junit.Test;

import static org.junit.Assert.*;

public class EmailTypeTest {

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
}
