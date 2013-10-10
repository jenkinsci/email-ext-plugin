package hudson.plugins.emailext;
import org.junit.Test;

import static org.junit.Assert.*;

public class EmailTypeTest {

    @Test
    public void testHasNoRecipients() {
        EmailType t = new EmailType();

        t.setSendToRecipientList(false);
        t.setSendToDevelopers(false);

        assertFalse(t.getHasRecipients());
    }

    @Test
    public void testHasDeveloperRecipients() {
        EmailType t = new EmailType();

        t.setSendToRecipientList(false);
        t.setSendToDevelopers(true);

        assertTrue(t.getHasRecipients());
    }

    @Test
    public void testHasRecipientList() {
        EmailType t = new EmailType();

        t.setSendToRecipientList(true);
        t.setSendToDevelopers(false);

        assertTrue(t.getHasRecipients());
    }

    @Test
    public void testHasDeveloperAndRecipientList() {
        EmailType t = new EmailType();

        t.setSendToRecipientList(true);
        t.setSendToDevelopers(true);

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
