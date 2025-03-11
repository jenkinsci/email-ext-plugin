package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;

class RecipientListStringAnalyserTest {

    @Test
    void getTypeForNotContainedEmailAddressReturnsMinus1() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("mickey@disney.com");
        assertEquals(
                RecipientListStringAnalyser.NOT_FOUND, analyser.getType(new InternetAddress("mickey2@disney.com")));
    }

    @Test
    void getTypeForEmailAddressReturnsTO() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("mickey@disney.com");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    void getTypeForCcPrefixedEmailAddressReturnsCC() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("cc:mickey@disney.com");
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    void getTypeForEmailAddressStartingWithCCReturnsTo() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("ccmickey@disney.com");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("ccmickey@disney.com")));
    }

    @Test
    void getTypeForBccPrefixedEmailAddressReturnsBCC() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser("bcc:mickey@disney.com");
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    void getTypeForSeveralEmailAddressesWithDifferentSeparatorsReturnsTO() throws Exception {
        String[] testStrings = {
            "mickey@disney.com;donald@disney.com",
            "mickey@disney.com donald@disney.com",
            "mickey@disney.com,donald@disney.com",
            "mickey@disney.com, donald@disney.com"
        };
        for (String testString : testStrings) {
            RecipientListStringAnalyser analyser = new RecipientListStringAnalyser(testString);
            assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
            assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("donald@disney.com")));
        }
    }

    @Test
    void getTypeForEmailAddressWithWhitespaceBeforeAndAfterReturnsTO() throws Exception {
        RecipientListStringAnalyser analyser = new RecipientListStringAnalyser(" mickey@disney.com ");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    void getTypeForPartiallyCcPrefixed2ndEmailAddresses() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("mickey@disney.com, cc:donald@disney.com");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
    }

    @Test
    void getTypeForPartiallyCcPrefixed1stEmailAddresses() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("cc:donald@disney.com, mickey@disney.com");
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    void getTypeForPartiallyBccPrefixed2ndEmailAddresses() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("mickey@disney.com, bcc:donald@disney.com");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("donald@disney.com")));
    }

    @Test
    void getTypeForPartiallyBccPrefixed1stEmailAddresses() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("bcc:donald@disney.com, mickey@disney.com");
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("donald@disney.com")));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    void getTypeForCcAndBccPrefixedEmailAddresses() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("cc:mickey@disney.com, bcc:donald@disney.com");
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("mickey@disney.com")));
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("donald@disney.com")));
    }

    @Test
    void getTypeForBccAndCcPrefixedEmailAddresses() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("bcc:donald@disney.com, cc:mickey@disney.com");
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("donald@disney.com")));
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    /*
     * Calls to {@link RecipientListStringAnalyser#getType(InternetAddress)} must be in right order; in order to avoid
     * bugs in test case logic each permutation is implemented (again) in its own test case below.
     */
    @Test
    void getTypeForPartiallyCcAndBccPrefixedEmailAddresses1() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("mickey@disney.com, cc:donald@disney.com, bcc:goofy@disney.com");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("goofy@disney.com")));
    }

    @Test
    void getTypeForPartiallyCcAndBccPrefixedEmailAddresses2() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("mickey@disney.com, bcc:goofy@disney.com, cc:donald@disney.com");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("goofy@disney.com")));
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
    }

    @Test
    void getTypeForPartiallyCcAndBccPrefixedEmailAddresses3() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("cc:donald@disney.com, mickey@disney.com, bcc:goofy@disney.com");
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("goofy@disney.com")));
    }

    @Test
    void getTypeForPartiallyCcAndBccPrefixedEmailAddresses4() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("cc:donald@disney.com, bcc:goofy@disney.com, mickey@disney.com");
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("goofy@disney.com")));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    void getTypeForPartiallyCcAndBccPrefixedEmailAddresses5() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("bcc:goofy@disney.com, mickey@disney.com, cc:donald@disney.com");
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("goofy@disney.com")));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
    }

    @Test
    void getTypeForPartiallyCcAndBccPrefixedEmailAddresses6() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("bcc:goofy@disney.com, cc:donald@disney.com, mickey@disney.com");
        assertEquals(EmailRecipientUtils.BCC, analyser.getType(new InternetAddress("goofy@disney.com")));
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("donald@disney.com")));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("mickey@disney.com")));
    }

    @Test
    void testUTF8() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("ashlux@gmail.com, cc:slide.o.mix@gmail.com, 愛嬋 <another@gmail.com>");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("ashlux@gmail.com")));
        assertEquals(EmailRecipientUtils.CC, analyser.getType(new InternetAddress("slide.o.mix@gmail.com")));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("another@gmail.com")));
    }

    @Test
    void testBackwardsNames() throws Exception {
        RecipientListStringAnalyser analyser =
                new RecipientListStringAnalyser("\"Mouse, Mickey\" <mickey@disney.com>, minnie@disney.com");
        InternetAddress addressWithPersonal = new InternetAddress("mickey@disney.com");
        addressWithPersonal.setPersonal("\"Mouse, Mickey\"");
        assertEquals(EmailRecipientUtils.TO, analyser.getType(addressWithPersonal));
        assertEquals(EmailRecipientUtils.TO, analyser.getType(new InternetAddress("minnie@disney.com")));
    }
}
