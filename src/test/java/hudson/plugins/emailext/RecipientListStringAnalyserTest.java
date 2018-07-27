package hudson.plugins.emailext;

import org.junit.Test;

import javax.mail.internet.InternetAddress;

import static org.junit.Assert.assertEquals;

public class RecipientListStringAnalyserTest {

    @Test
    public void getTypeForNotContainedEmailAddressReturnsMinus1() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("mickey@disney.com");
        assertEquals(RecipientListStringAnalyser.NOT_FOUND, analyser.getType(new InternetAddress("mickey2@disney.com")));
    }

    @Test
    public void getTypeForEmailAddressReturnsTO() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("mickey@disney.com");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    public void getTypeForCcPrefixedEmailAddressReturnsCC() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("cc:mickey@disney.com");
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    public void getTypeForBccPrefixedEmailAddressReturnsBCC() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("bcc:mickey@disney.com");
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    public void getTypeForSeveralEmailAddressesWithDifferentSeparatorsReturnsTO() throws Exception {
        String[] testStrings = {
                "mickey@disney.com;donald@disney.com",
                "mickey@disney.com donald@disney.com",
                "mickey@disney.com,donald@disney.com",
                "mickey@disney.com, donald@disney.com"
        };
        for(String testString : testStrings) {
            RecipientListStringAnalyser analyser = new RecipientListStringAnalyser(testString);
            assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
            assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("donald@disney.com")));
        }
    }

    @Test
    public void getTypeForEmailAddressWithWhitespaceBeforeAndAfterReturnsTO() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser(" mickey@disney.com ");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    public void getTypeForPartiallyCcPrefixedEmailAddresses() throws Exception {
        String[] testStrings = {
                "mickey@disney.com, cc:donald@disney.com",
                "cc:donald@disney.com, mickey@disney.com"
        };
        for(String testString : testStrings) {
            RecipientListStringAnalyser analyser = new RecipientListStringAnalyser(testString);
            assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
            assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
        }
    }

    @Test
    public void getTypeForPartiallyBccPrefixedEmailAddresses() throws Exception {
        String[] testStrings = {
                "mickey@disney.com, bcc:donald@disney.com",
                "bcc:donald@disney.com, mickey@disney.com"
        };
        for(String testString : testStrings) {
            RecipientListStringAnalyser analyser = new RecipientListStringAnalyser(testString);
            assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
            assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("donald@disney.com")));
        }
    }

    @Test
    public void getTypeForCcAndBccPrefixedEmailAddresses() throws Exception {
        String[] testStrings = {
                "cc:mickey@disney.com, bcc:donald@disney.com",
                "bcc:donald@disney.com, cc:mickey@disney.com"
        };
        for(String testString : testStrings) {
            RecipientListStringAnalyser analyser = new RecipientListStringAnalyser(testString);
            assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("mickey@disney.com")));
            assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("donald@disney.com")));
        }
    }

    @Test
    public void getTypeForPartiallyCcAndBccPrefixedEmailAddresses() throws Exception {
        String[] testStrings = {
                "mickey@disney.com, cc:donald@disney.com, bcc:goofy@disney.com",
                "mickey@disney.com, bcc:goofy@disney.com, cc:donald@disney.com",
                "cc:donald@disney.com, mickey@disney.com, bcc:goofy@disney.com",
                "cc:donald@disney.com, bcc:goofy@disney.com, mickey@disney.com",
                "bcc:goofy@disney.com, mickey@disney.com, cc:donald@disney.com",
                "bcc:goofy@disney.com, cc:donald@disney.com, mickey@disney.com"
        };
        for(String testString : testStrings) {
            RecipientListStringAnalyser analyser = new RecipientListStringAnalyser(testString);
            assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
            assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
            assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("goofy@disney.com")));
        }
    }

    @Test
    public void testUTF8() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("ashlux@gmail.com, cc:slide.o.mix@gmail.com, 愛嬋 <another@gmail.com>");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("ashlux@gmail.com")));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("another@gmail.com")));
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("slide.o.mix@gmail.com")));
    }

    @Test
    public void testBackwardsNames() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("\"Mouse, Mickey\" <mickey@disney.com>, minnie@disney.com");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("minnie@disney.com")));
    }

}