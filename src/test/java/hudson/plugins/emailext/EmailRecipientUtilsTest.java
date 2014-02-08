package hudson.plugins.emailext;

import hudson.EnvVars;
import hudson.model.User;
import hudson.util.FormValidation;
import hudson.tasks.Mailer;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Set;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;

import org.jvnet.hudson.test.JenkinsRule;

public class EmailRecipientUtilsTest {

    @Rule 
    public JenkinsRule j = new JenkinsRule() {
        @Override
        protected void before() throws Throwable {
            emailRecipientUtils = new EmailRecipientUtils();
            envVars = new EnvVars();
            super.before();
        }        
    };
    
    private EmailRecipientUtils emailRecipientUtils;
    private EnvVars envVars;

    @Test
    public void testConvertRecipientList_emptyRecipientStringShouldResultInEmptyEmailList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString("", envVars);

        assertTrue(internetAddresses.isEmpty());
    }

    @Test
    public void testConvertRecipientList_emptyRecipientStringWithWhitespaceShouldResultInEmptyEmailList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString("   ", envVars);

        assertTrue(internetAddresses.isEmpty());
    }

    @Test
    public void testConvertRecipientList_singleRecipientShouldResultInOneEmailAddressInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                emailRecipientUtils.convertRecipientString("ashlux@gmail.com", envVars);

        assertEquals(1, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
    }

    @Test
    public void testConvertRecipientList_singleRecipientWithWhitespaceShouldResultInOneEmailAddressInList()
            throws AddressException, UnsupportedEncodingException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                emailRecipientUtils.convertRecipientString(" ashlux@gmail.com ", envVars);

        assertEquals(1, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
    }

    @Test
    public void testConvertRecipientList_commaSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                emailRecipientUtils.convertRecipientString("ashlux@gmail.com, mickeymouse@disney.com", envVars);

        assertEquals(2, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
        assertTrue(internetAddresses.contains(new InternetAddress("mickeymouse@disney.com")));
    }

    @Test
    public void testConvertRecipientList_spaceSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                emailRecipientUtils.convertRecipientString("ashlux@gmail.com mickeymouse@disney.com", envVars);

        assertEquals(2, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
        assertTrue(internetAddresses.contains(new InternetAddress("mickeymouse@disney.com")));
    }

    @Test
    public void testConvertRecipientList_emailAddressesShouldBeUnique()
            throws AddressException, UnsupportedEncodingException {
        Set<InternetAddress> internetAddresses =
                emailRecipientUtils.convertRecipientString("ashlux@gmail.com, mickeymouse@disney.com, ashlux@gmail.com", envVars);

        assertEquals(2, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
        assertTrue(internetAddresses.contains(new InternetAddress("mickeymouse@disney.com")));
    }

    @Test
    public void testConvertRecipientList_recipientStringShouldBeExpanded()
            throws AddressException, UnsupportedEncodingException {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com");

        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(1, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
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
        Mailer.descriptor().setDefaultSuffix("@gmail.com");
        InternetAddress[] internetAddresses = emailRecipientUtils.convertRecipientString("ashlux", envVars).toArray(new InternetAddress[0]);

        assertEquals(1, internetAddresses.length);
        assertEquals("ashlux@gmail.com", internetAddresses[0].getAddress());
    }

    @Test
    public void testConvertRecipientList_userName()
            throws AddressException, IOException, UnsupportedEncodingException {
        Mailer.descriptor().setDefaultSuffix("@gmail.com");
        User u = User.get("advantiss");
        u.setFullName("Peter Samoshkin");
        Mailer.UserProperty prop = new Mailer.UserProperty("advantiss@xxx.com");
        u.addProperty(prop);

        InternetAddress[] internetAddresses = emailRecipientUtils.convertRecipientString("advantiss", envVars).toArray(new InternetAddress[0]);

        assertEquals(1, internetAddresses.length);
        assertEquals("advantiss@xxx.com", internetAddresses[0].getAddress());
    }

    @Test
    public void testCC()
            throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, cc:slide.o.mix@gmail.com, another@gmail.com");

        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(2, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
        assertTrue(internetAddresses.contains(new InternetAddress("another@gmail.com")));

        internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.CC);

        assertEquals(1, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("slide.o.mix@gmail.com")));
    }

    @Test
    public void testBCC()
            throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, bcc:slide.o.mix@gmail.com, another@gmail.com");

        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(2, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
        assertTrue(internetAddresses.contains(new InternetAddress("another@gmail.com")));

        internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.BCC);

        assertEquals(1, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("slide.o.mix@gmail.com")));
    }

    @Test
    public void testBCCandCC()
            throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, bcc:slide.o.mix@gmail.com, another@gmail.com, cc:example@gmail.com");

        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);

        assertEquals(2, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
        assertTrue(internetAddresses.contains(new InternetAddress("another@gmail.com")));

        internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.BCC);

        assertEquals(1, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("slide.o.mix@gmail.com")));

        internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.CC);

        assertEquals(1, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("example@gmail.com")));
    }

    @Test
    public void testUTF8()
            throws Exception {
        envVars.put("EMAIL_LIST", "ashlux@gmail.com, cc:slide.o.mix@gmail.com, 愛嬋 <another@gmail.com>");

        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars);
        assertEquals(2, internetAddresses.size());

        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
        InternetAddress addr = new InternetAddress("another@gmail.com");
        addr.setPersonal(MimeUtility.encodeWord("愛嬋", "UTF-8", "B"));
        assertTrue(internetAddresses.contains(addr));

        internetAddresses = emailRecipientUtils.convertRecipientString("$EMAIL_LIST", envVars, EmailRecipientUtils.CC);

        assertEquals(1, internetAddresses.size());
        assertTrue(internetAddresses.contains(new InternetAddress("slide.o.mix@gmail.com")));
    }
}
