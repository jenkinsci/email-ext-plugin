package hudson.plugins.emailext;

import hudson.EnvVars;
import hudson.model.User;
import hudson.util.FormValidation;
import hudson.tasks.Mailer;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.Set;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.jvnet.hudson.test.HudsonTestCase;

public class EmailRecipientUtilsTest extends HudsonTestCase
{
    private EmailRecipientUtils emailRecipientUtils;

    private EnvVars envVars;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        emailRecipientUtils = new EmailRecipientUtils();
        envVars = new EnvVars();
    }

    public void testConvertRecipientList_emptyRecipientStringShouldResultInEmptyEmailList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString( "", envVars );

        assertTrue( internetAddresses.isEmpty() );
    }

    public void testConvertRecipientList_emptyRecipientStringWithWhitespaceShouldResultInEmptyEmailList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString( "   ", envVars );

        assertTrue( internetAddresses.isEmpty() );
    }

    public void testConvertRecipientList_singleRecipientShouldResultInOneEmailAddressInList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecipientUtils.convertRecipientString( "ashlux@gmail.com", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
    }

    public void testConvertRecipientList_singleRecipientWithWhitespaceShouldResultInOneEmailAddressInList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecipientUtils.convertRecipientString( " ashlux@gmail.com ", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
    }

    public void testConvertRecipientList_commaSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecipientUtils.convertRecipientString( "ashlux@gmail.com, mickeymouse@disney.com", envVars );

        assertEquals( 2, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
        assertTrue( internetAddresses.contains( new InternetAddress( "mickeymouse@disney.com" ) ) );
    }

    public void testConvertRecipientList_spaceSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecipientUtils.convertRecipientString( "ashlux@gmail.com mickeymouse@disney.com", envVars );

        assertEquals( 2, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
        assertTrue( internetAddresses.contains( new InternetAddress( "mickeymouse@disney.com" ) ) );
    }

    public void testConvertRecipientList_emailAddressesShouldBeUnique()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecipientUtils.convertRecipientString( "ashlux@gmail.com, mickeymouse@disney.com, ashlux@gmail.com", envVars );

        assertEquals( 2, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
        assertTrue( internetAddresses.contains( new InternetAddress( "mickeymouse@disney.com" ) ) );
    }

    public void testConvertRecipientList_recipientStringShouldBeExpanded()
        throws AddressException
    {
        envVars.put( "EMAIL_LIST", "ashlux@gmail.com" );

        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString( "$EMAIL_LIST", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
    }

    public void testValidateFormRecipientList_validationShouldPassAListOfGoodEmailAddresses()
    {
        FormValidation formValidation =
            emailRecipientUtils.validateFormRecipientList( "ashlux@gmail.com internal somewhere@domain" );

        assertEquals( FormValidation.Kind.OK, formValidation.kind );
    }
    
    public void testValidateFormRecipientList_validationShouldFailWithBadEmailAddress()
    {
        FormValidation formValidation =
            emailRecipientUtils.validateFormRecipientList( "@@@" );

        assertEquals( FormValidation.Kind.ERROR, formValidation.kind );
    }

    public void testConvertRecipientList_defaultSuffix()
        throws AddressException
    {
        Mailer.descriptor().setDefaultSuffix("@gmail.com");
        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString( "ashlux", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertTrue(internetAddresses.contains(new InternetAddress("ashlux@gmail.com")));
    }

    public void testConvertRecipientList_userName()
            throws AddressException, IOException {
        Mailer.descriptor().setDefaultSuffix("@gmail.com");
        User u = User.get("advantiss");
        u.setFullName("Peter Samoshkin");
        Mailer.UserProperty prop = new Mailer.UserProperty("advantiss@xxx.com");
        u.addProperty(prop);

        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString( "advantiss", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "advantiss@xxx.com" ) ) );
    }

    public void testCC()
        throws Exception
    {
        envVars.put( "EMAIL_LIST", "ashlux@gmail.com, cc:slide.o.mix@gmail.com, another@gmail.com" );

        Set<InternetAddress> internetAddresses = emailRecipientUtils.convertRecipientString( "$EMAIL_LIST", envVars );

        assertEquals( 2, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
        assertTrue( internetAddresses.contains( new InternetAddress( "another@gmail.com" ) ) );

        internetAddresses = emailRecipientUtils.convertRecipientString( "$EMAIL_LIST", envVars, EmailRecipientUtils.CC);
        
        assertEquals( 1, internetAddresses.size() );
        assertTrue ( internetAddresses.contains( new InternetAddress( "slide.o.mix@gmail.com" ) ) );
    }
}
