package hudson.plugins.emailext.recipients;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.EnvVars;
import hudson.model.User;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.tasks.Mailer;
import hudson.util.FormValidation;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class EmailRecipientUtilsTest {

    private EmailRecipientUtils emailRecipientUtils;
    private EnvVars envVars;

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
        emailRecipientUtils = new EmailRecipientUtils();
        envVars = new EnvVars();
    }

    @Test
    @Issue("JENKINS-32889")
    void testDelimiters() throws Exception {
        String[] testStrings = {
            "mickey@disney.com;donald@disney.com;goofy@disney.com;pluto@disney.com",
            "mickey@disney.com donald@disney.com goofy@disney.com pluto@disney.com",
            "mickey@disney.com,donald@disney.com,goofy@disney.com,pluto@disney.com"
        };
        for (String testString : testStrings) {
            Set<InternetAddress> addresses =
                    EmailRecipientUtils.convertRecipientString(testString, envVars, EmailRecipientUtils.TO);

            assertEquals(
                    setOf("mickey@disney.com", "donald@disney.com", "goofy@disney.com", "pluto@disney.com"), addresses);
        }
    }

    @Test
    void testConvertRecipientList_emptyRecipientStringShouldResultInEmptyEmailList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("", envVars);

        assertThat(internetAddresses, is(empty()));
    }

    @Test
    void testConvertRecipientList_emptyRecipientStringWithWhitespaceShouldResultInEmptyEmailList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("   ", envVars);

        assertThat(internetAddresses, is(empty()));
    }

    @Test
    void testConvertRecipientList_singleRecipientShouldResultInOneEmailAddressInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                EmailRecipientUtils.convertRecipientString("ashlux@gmail.com", envVars);

        assertEquals(setOf("ashlux@gmail.com"), internetAddresses);
    }

    @Test
    void testConvertRecipientList_singleRecipientWithWhitespaceShouldResultInOneEmailAddressInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                EmailRecipientUtils.convertRecipientString(" ashlux@gmail.com ", envVars);

        assertEquals(setOf("ashlux@gmail.com"), internetAddresses);
    }

    @Test
    void testConvertRecipientList_commaSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                EmailRecipientUtils.convertRecipientString("ashlux@gmail.com, mickeymouse@disney.com", envVars);

        assertEquals(setOf("ashlux@gmail.com", "mickeymouse@disney.com"), internetAddresses);
    }

    @Test
    void testConvertRecipientList_spaceSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                EmailRecipientUtils.convertRecipientString("ashlux@gmail.com mickeymouse@disney.com", envVars);

        assertEquals(setOf("ashlux@gmail.com", "mickeymouse@disney.com"), internetAddresses);
    }

    @Test
    void testConvertRecipientList_emailAddressesShouldBeUnique() throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString(
                "ashlux@gmail.com, mickeymouse@disney.com, ashlux@gmail.com", envVars);

        assertEquals(setOf("ashlux@gmail.com", "mickeymouse@disney.com"), internetAddresses);
    }

    @Test
    void testConvertRecipientList_recipientStringShouldBeExpanded()
            throws AddressException, UnsupportedEncodingException {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com"), internetAddresses);
    }

    @Test
    void testValidateFormRecipientList_validationShouldPassAListOfGoodEmailAddresses() {
        FormValidation formValidation =
                emailRecipientUtils.validateFormRecipientList("ashlux@gmail.com internal somewhere@domain");

        assertEquals(FormValidation.Kind.OK, formValidation.kind);
    }

    @Test
    void testValidateFormRecipientList_validationShouldFailWithBadEmailAddress() {
        FormValidation formValidation = emailRecipientUtils.validateFormRecipientList("@@@");

        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    }

    @Test
    void testConvertRecipientList_defaultSuffix() throws AddressException, UnsupportedEncodingException {
        ExtendedEmailPublisher.descriptor().setDefaultSuffix("@gmail.com");
        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("ashlux", envVars);

        assertEquals(setOf("ashlux@gmail.com"), internetAddresses);
    }

    @Test
    void testConvertRecipientList_userName() throws AddressException, IOException {
        ExtendedEmailPublisher.descriptor().setDefaultSuffix("@gmail.com");
        User u = User.getById("advantiss", true);
        u.setFullName("Peter Samoshkin");
        Mailer.UserProperty prop = new Mailer.UserProperty("advantiss@xxx.com");
        u.addProperty(prop);

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("advantiss", envVars);

        assertEquals(setOf("advantiss@xxx.com"), internetAddresses);
    }

    @Test
    void testCC() throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, cc:slide.o.mix@gmail.com, another@gmail.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com", "another@gmail.com"), internetAddresses);

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.CC);

        assertEquals(setOf("slide.o.mix@gmail.com"), internetAddresses);
    }

    @Test
    void testBCC() throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, bcc:slide.o.mix@gmail.com, another@gmail.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com", "another@gmail.com"), internetAddresses);

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.BCC);

        assertEquals(setOf("slide.o.mix@gmail.com"), internetAddresses);
    }

    @Test
    void testBCCandCC() throws Exception {
        envVars.put(
                "EMAIL_LIST", "ashlux@gmail.com, bcc:slide.o.mix@gmail.com, another@gmail.com, cc:example@gmail.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com", "another@gmail.com"), internetAddresses);

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.BCC);

        assertEquals(setOf("slide.o.mix@gmail.com"), internetAddresses);

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.CC);

        assertEquals(setOf("example@gmail.com"), internetAddresses);
    }

    @Test
    void testSameRecipientInTOandCCandBCC_ShouldNotBeFilteredFromTOandCC() throws Exception {
        String recipientListString = "user@gmail.com, bcc:user@gmail.com, cc:user@gmail.com";

        Set<InternetAddress> toInternetAddresses =
                EmailRecipientUtils.convertRecipientString(recipientListString, envVars);

        assertEquals(setOf("user@gmail.com"), toInternetAddresses);

        Set<InternetAddress> ccInternetAddresses =
                EmailRecipientUtils.convertRecipientString(recipientListString, envVars, EmailRecipientUtils.CC);

        assertEquals(setOf("user@gmail.com"), ccInternetAddresses);

        Set<InternetAddress> bccInternetAddresses =
                EmailRecipientUtils.convertRecipientString(recipientListString, envVars, EmailRecipientUtils.BCC);

        assertEquals(setOf("user@gmail.com"), bccInternetAddresses);
    }

    @Test
    void testUTF8() throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, cc:slide.o.mix@gmail.com, 愛嬋 <another@gmail.com>");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com", "another@gmail.com"), internetAddresses);
        assertEquals("愛嬋", ((InternetAddress) CollectionUtils.get(internetAddresses, 1)).getPersonal());

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.CC);

        assertEquals(setOf("slide.o.mix@gmail.com"), internetAddresses);
    }

    @Test
    void testBackwardsNames() throws Exception {
        envVars.put("EMAIL_LIST", "\"Mouse, Mickey\" <mickey@disney.com>, minnie@disney.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);
        assertEquals(setOf("mickey@disney.com", "minnie@disney.com"), internetAddresses);
        assertEquals("Mouse, Mickey", ((InternetAddress) CollectionUtils.get(internetAddresses, 0)).getPersonal());
    }

    // Test helper:

    private static Set<InternetAddress> setOf(String... emailAddresses) throws AddressException {
        final Set<InternetAddress> addresses = new LinkedHashSet<>();
        for (String emailAddress : emailAddresses) {
            addresses.add(new InternetAddress(emailAddress));
        }
        return addresses;
    }
}
