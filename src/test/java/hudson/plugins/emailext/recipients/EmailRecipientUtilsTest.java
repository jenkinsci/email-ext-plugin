package hudson.plugins.emailext.recipients;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import hudson.EnvVars;
import hudson.model.User;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.tasks.Mailer;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

public class EmailRecipientUtilsTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() {
        emailRecipientUtils = new EmailRecipientUtils();
        envVars = new EnvVars();
    }

    private EmailRecipientUtils emailRecipientUtils;
    private EnvVars envVars;

    @Test
    @Issue("JENKINS-32889")
    public void testDelimiters() throws Exception {
        String[] testStrings = { "mickey@disney.com;donald@disney.com;goofy@disney.com;pluto@disney.com",
                "mickey@disney.com donald@disney.com goofy@disney.com pluto@disney.com",
                "mickey@disney.com,donald@disney.com,goofy@disney.com,pluto@disney.com" };
        for(String testString : testStrings) {
            Set<InternetAddress> addresses = EmailRecipientUtils.convertRecipientString(testString, envVars, EmailRecipientUtils.TO);

            assertEquals(setOf("mickey@disney.com", "donald@disney.com", "goofy@disney.com", "pluto@disney.com"), addresses);
        }
    }

    @Test
    public void testConvertRecipientList_emptyRecipientStringShouldResultInEmptyEmailList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("", envVars);

        assertThat(internetAddresses, is(empty()));
    }

    @Test
    public void testConvertRecipientList_emptyRecipientStringWithWhitespaceShouldResultInEmptyEmailList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("   ", envVars);

        assertThat(internetAddresses, is(empty()));
    }

    @Test
    public void testConvertRecipientList_singleRecipientShouldResultInOneEmailAddressInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                EmailRecipientUtils.convertRecipientString("ashlux@gmail.com", envVars);

        assertEquals(setOf("ashlux@gmail.com"), internetAddresses);
    }

    @Test
    public void testConvertRecipientList_singleRecipientWithWhitespaceShouldResultInOneEmailAddressInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                EmailRecipientUtils.convertRecipientString(" ashlux@gmail.com ", envVars);

        assertEquals(setOf("ashlux@gmail.com"), internetAddresses);
    }

    @Test
    public void testConvertRecipientList_commaSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                EmailRecipientUtils.convertRecipientString("ashlux@gmail.com, mickeymouse@disney.com", envVars);

        assertEquals(setOf("ashlux@gmail.com", "mickeymouse@disney.com"), internetAddresses);
    }

    @Test
    public void testConvertRecipientList_spaceSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                EmailRecipientUtils.convertRecipientString("ashlux@gmail.com mickeymouse@disney.com", envVars);

        assertEquals(setOf("ashlux@gmail.com", "mickeymouse@disney.com"), internetAddresses);
    }

    @Test
    public void testConvertRecipientList_emailAddressesShouldBeUnique()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                EmailRecipientUtils.convertRecipientString("ashlux@gmail.com, mickeymouse@disney.com, ashlux@gmail.com", envVars);

        assertEquals(setOf("ashlux@gmail.com", "mickeymouse@disney.com"), internetAddresses);
    }

    @Test
    public void testConvertRecipientList_recipientStringShouldBeExpanded()
            throws AddressException, UnsupportedEncodingException {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com"), internetAddresses);
    }

    @Test
    public void testValidateFormRecipientList_validationShouldPassAListOfGoodEmailAddresses() {
        FormValidation formValidation =
                emailRecipientUtils.validateFormRecipientList("ashlux@gmail.com internal somewhere@domain");

        assertEquals(FormValidation.Kind.OK, formValidation.kind);
    }

    @Test
    public void testValidateFormRecipientList_validationShouldFailWithBadEmailAddress() {
        FormValidation formValidation =
                emailRecipientUtils.validateFormRecipientList("@@@");

        assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    }

    @Test
    public void testConvertRecipientList_defaultSuffix()
            throws AddressException, UnsupportedEncodingException {
        ExtendedEmailPublisher.descriptor().setDefaultSuffix("@gmail.com");
        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("ashlux", envVars);

        assertEquals(setOf("ashlux@gmail.com"), internetAddresses);
    }

    @Test
    public void testConvertRecipientList_userName()
            throws AddressException, IOException {
        ExtendedEmailPublisher.descriptor().setDefaultSuffix("@gmail.com");
        User u = User.getById("advantiss", true);
        u.setFullName("Peter Samoshkin");
        Mailer.UserProperty prop = new Mailer.UserProperty("advantiss@xxx.com");
        u.addProperty(prop);

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("advantiss", envVars);

        assertEquals(setOf("advantiss@xxx.com"), internetAddresses);
    }

    @Test
    public void testCC()
            throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, cc:slide.o.mix@gmail.com, another@gmail.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com", "another@gmail.com"), internetAddresses);

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.CC);

        assertEquals(setOf("slide.o.mix@gmail.com"), internetAddresses);
    }

    @Test
    public void testBCC()
            throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, bcc:slide.o.mix@gmail.com, another@gmail.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com", "another@gmail.com"), internetAddresses);

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.BCC);

        assertEquals(setOf("slide.o.mix@gmail.com"), internetAddresses);
    }

    @Test
    public void testBCCandCC()
            throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, bcc:slide.o.mix@gmail.com, another@gmail.com, cc:example@gmail.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com", "another@gmail.com"), internetAddresses);

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.BCC);

        assertEquals(setOf("slide.o.mix@gmail.com"), internetAddresses);

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.CC);

        assertEquals(setOf("example@gmail.com"), internetAddresses);
    }

    @Test
    public void testSameRecipientInTOandCCandBCC_ShouldNotBeFilteredFromTOandCC()
            throws Exception {
        String recipientListString = "user@gmail.com, bcc:user@gmail.com, cc:user@gmail.com";

        Set<InternetAddress> toInternetAddresses = EmailRecipientUtils.convertRecipientString(recipientListString, envVars);

        assertEquals(setOf("user@gmail.com"), toInternetAddresses);

        Set<InternetAddress> ccInternetAddresses = EmailRecipientUtils.convertRecipientString(recipientListString, envVars, EmailRecipientUtils.CC);

        assertEquals(setOf("user@gmail.com"), ccInternetAddresses);

        Set<InternetAddress> bccInternetAddresses = EmailRecipientUtils.convertRecipientString(recipientListString, envVars, EmailRecipientUtils.BCC);

        assertEquals(setOf("user@gmail.com"), bccInternetAddresses);
    }

    @Test
    public void testUTF8()
            throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, cc:slide.o.mix@gmail.com, 愛嬋 <another@gmail.com>");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(setOf("ashlux@gmail.com", "another@gmail.com"), internetAddresses);
        assertEquals("愛嬋", ((InternetAddress) CollectionUtils.get(internetAddresses, 1)).getPersonal());

        internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.CC);

        assertEquals(setOf("slide.o.mix@gmail.com"), internetAddresses);
    }

    @Test
    public void testBackwardsNames() throws Exception {
        envVars.put("EMAIL_LIST", "\"Mouse, Mickey\" <mickey@disney.com>, minnie@disney.com");

        Set<InternetAddress> internetAddresses = EmailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);
        assertEquals(setOf("mickey@disney.com", "minnie@disney.com"), internetAddresses);
        assertEquals("Mouse, Mickey", ((InternetAddress) CollectionUtils.get(internetAddresses, 0)).getPersonal());
    }

    // Test helper:

    private Set<InternetAddress> setOf(String ... emailAddresses) throws AddressException {
        final Set<InternetAddress> addresses = new LinkedHashSet<>();
        for (String emailAddress : emailAddresses) {
            addresses.add(new InternetAddress(emailAddress));
        }
        return addresses;
    }

}
