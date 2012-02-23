package hudson.plugins.emailext;

import hudson.EnvVars;
import hudson.util.FormValidation;
import hudson.tasks.Mailer;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Set;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.jvnet.hudson.test.HudsonTestCase;

public class EmailRecepientUtilsTest extends HudsonTestCase
{
    private EmailRecepientUtils emailRecepientUtils;

    private EnvVars envVars;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        emailRecepientUtils = new EmailRecepientUtils();
        envVars = new EnvVars();
    }

    public void testConvertRecipientList_emptyRecipientStringShouldResultInEmptyEmailList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses = emailRecepientUtils.convertRecipientString( "", envVars );

        assertTrue( internetAddresses.isEmpty() );
    }

    public void testConvertRecipientList_emptyRecipientStringWithWhitespaceShouldResultInEmptyEmailList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses = emailRecepientUtils.convertRecipientString( "   ", envVars );

        assertTrue( internetAddresses.isEmpty() );
    }

    public void testConvertRecipientList_singleRecipientShouldResultInOneEmailAddressInList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecepientUtils.convertRecipientString( "ashlux@gmail.com", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
    }

    public void testConvertRecipientList_singleRecipientWithWhitespaceShouldResultInOneEmailAddressInList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecepientUtils.convertRecipientString( " ashlux@gmail.com ", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
    }

    public void testConvertRecipientList_commaSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecepientUtils.convertRecipientString( "ashlux@gmail.com, mickeymouse@disney.com", envVars );

        assertEquals( 2, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
        assertTrue( internetAddresses.contains( new InternetAddress( "mickeymouse@disney.com" ) ) );
    }

    public void testConvertRecipientList_spaceSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecepientUtils.convertRecipientString( "ashlux@gmail.com mickeymouse@disney.com", envVars );

        assertEquals( 2, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
        assertTrue( internetAddresses.contains( new InternetAddress( "mickeymouse@disney.com" ) ) );
    }

    public void testConvertRecipientList_emailAddressesShouldBeUnique()
        throws AddressException
    {
        Set<InternetAddress> internetAddresses =
            emailRecepientUtils.convertRecipientString( "ashlux@gmail.com, mickeymouse@disney.com, ashlux@gmail.com", envVars );

        assertEquals( 2, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
        assertTrue( internetAddresses.contains( new InternetAddress( "mickeymouse@disney.com" ) ) );
    }

    public void testConvertRecipientList_recipientStringShouldBeExpanded()
        throws AddressException
    {
        envVars.put( "EMAIL_LIST", "ashlux@gmail.com" );

        Set<InternetAddress> internetAddresses = emailRecepientUtils.convertRecipientString( "$EMAIL_LIST", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
    }

    public void testValidateFormRecipientList_validationShouldPassAListOfGoodEmailAddresses()
    {
        FormValidation formValidation =
            emailRecepientUtils.validateFormRecipientList( "ashlux@gmail.com internal somewhere@domain" );

        assertEquals( FormValidation.Kind.OK, formValidation.kind );
    }
    
    public void testValidateFormRecipientList_validationShouldFailWithBadEmailAddress()
    {
        FormValidation formValidation =
            emailRecepientUtils.validateFormRecipientList( "@@@" );

        assertEquals( FormValidation.Kind.ERROR, formValidation.kind );
    }

    public void testConvertRecipientList_defaultSuffix()
        throws AddressException
    {
        Mailer.descriptor().setDefaultSuffix("@gmail.com");
        Set<InternetAddress> internetAddresses = emailRecepientUtils.convertRecipientString( "ashlux", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertTrue( internetAddresses.contains( new InternetAddress( "ashlux@gmail.com" ) ) );
    }
}
