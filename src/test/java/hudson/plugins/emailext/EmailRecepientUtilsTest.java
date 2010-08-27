package hudson.plugins.emailext;

import hudson.EnvVars;
import hudson.util.FormValidation;
import org.junit.Before;
import org.junit.Test;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class EmailRecepientUtilsTest
{
    private EmailRecepientUtils emailRecepientUtils;

    private EnvVars envVars;

    @Before
    public void before()
    {
        emailRecepientUtils = new EmailRecepientUtils();

        envVars = new EnvVars();
    }

    @Test
    public void testConvertRecipientList_emptyRecipientStringShouldResultInEmptyEmailList()
        throws AddressException
    {
        List<InternetAddress> internetAddresses = emailRecepientUtils.convertRecipientString( "", envVars );

        assertTrue( internetAddresses.isEmpty() );
    }

    @Test
    public void testConvertRecipientList_emptyRecipientStringWithWhitespaceShouldResultInEmptyEmailList()
        throws AddressException
    {
        List<InternetAddress> internetAddresses = emailRecepientUtils.convertRecipientString( "   ", envVars );

        assertTrue( internetAddresses.isEmpty() );
    }

    @Test
    public void testConvertRecipientList_singleRecipientShouldResultInOneEmailAddressInList()
        throws AddressException
    {
        List<InternetAddress> internetAddresses =
            emailRecepientUtils.convertRecipientString( "ashlux@gmail.com", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertEquals( "ashlux@gmail.com", internetAddresses.get( 0 ).getAddress() );
    }

    @Test
    public void testConvertRecipientList_singleRecipientWithWhitespaceShouldResultInOneEmailAddressInList()
        throws AddressException
    {
        List<InternetAddress> internetAddresses =
            emailRecepientUtils.convertRecipientString( " ashlux@gmail.com ", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertEquals( "ashlux@gmail.com", internetAddresses.get( 0 ).getAddress() );
    }

    @Test
    public void testConvertRecipientList_commaSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
        throws AddressException
    {
        List<InternetAddress> internetAddresses =
            emailRecepientUtils.convertRecipientString( "ashlux@gmail.com, mickeymouse@disney.com", envVars );

        assertEquals( 2, internetAddresses.size() );
        assertEquals( "ashlux@gmail.com", internetAddresses.get( 0 ).getAddress() );
        assertEquals( "mickeymouse@disney.com", internetAddresses.get( 1 ).getAddress() );
    }

    @Test
    public void testConvertRecipientList_spaceSeparatedRecipientStringShouldResultInMultipleEmailAddressesInList()
        throws AddressException
    {
        List<InternetAddress> internetAddresses =
            emailRecepientUtils.convertRecipientString( "ashlux@gmail.com mickeymouse@disney.com", envVars );

        assertEquals( 2, internetAddresses.size() );
        assertEquals( "ashlux@gmail.com", internetAddresses.get( 0 ).getAddress() );
        assertEquals( "mickeymouse@disney.com", internetAddresses.get( 1 ).getAddress() );
    }

    @Test
    public void testConvertRecipientList_recipientStringShouldBeExpanded()
        throws AddressException
    {
        envVars.put( "EMAIL_LIST", "ashlux@gmail.com" );

        List<InternetAddress> internetAddresses = emailRecepientUtils.convertRecipientString( "$EMAIL_LIST", envVars );

        assertEquals( 1, internetAddresses.size() );
        assertEquals( "ashlux@gmail.com", internetAddresses.get( 0 ).getAddress() );
    }

    @Test
    public void testValidateFormRecipientList_validationShouldPassAListOfGoodEmailAddresses()
    {
        FormValidation formValidation =
            emailRecepientUtils.validateFormRecipientList( "ashlux@gmail.com internal somewhere@domain" );

        assertEquals( FormValidation.Kind.OK, formValidation.kind );
    }
    
    @Test
    public void testValidateFormRecipientList_validationShouldFailWithBadEmailAddress()
    {
        FormValidation formValidation =
            emailRecepientUtils.validateFormRecipientList( "@@@" );

        assertEquals( FormValidation.Kind.ERROR, formValidation.kind );
    }
}
