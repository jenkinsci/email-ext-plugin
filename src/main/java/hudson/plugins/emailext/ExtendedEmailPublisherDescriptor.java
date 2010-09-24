package hudson.plugins.emailext;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * These settings are global configurations
 */
public class ExtendedEmailPublisherDescriptor
    extends BuildStepDescriptor<Publisher>
{
    /**
     * The default e-mail address suffix appended to the user name found from changelog,
     * to send e-mails. Null if not configured.
     */
    private String defaultSuffix;

    /**
     * Hudson's own URL, to put into the e-mail.
     */
    private String hudsonUrl;

    /**
     * If non-null, use SMTP-AUTH
     */
    private String smtpAuthUsername;

    private Secret smtpAuthPassword;

    /**
     * The e-mail address that Hudson puts to "From:" field in outgoing e-mails.
     * Null if not configured.
     */
    private String adminAddress;

    /**
     * The SMTP server to use for sending e-mail. Null for default to the environment,
     * which is usually <tt>localhost</tt>.
     */
    private String smtpHost;

    /**
     * If true use SSL on port 465 (standard SMTPS) unless <code>smtpPort</code> is set.
     */
    private boolean useSsl;

    /**
     * The SMTP port to use for sending e-mail. Null for default to the environment,
     * which is usually <tt>25</tt>.
     */
    private String smtpPort;

    /**
     * This is a global default content type (mime type) for emails.
     */
    private String defaultContentType;

    /**
     * This is a global default subject line for sending emails.
     */
    private String defaultSubject;

    /**
     * This is a global default body for sending emails.
     */
    private String defaultBody;

    private boolean overrideGlobalSettings;

    @Override
    public String getDisplayName()
    {
        return "Editable Email Notification";
    }

    public String getAdminAddress()
    {
        String v = adminAddress;
        if ( v == null )
        {
            v = "address not configured yet <nobody>";
        }
        return v;
    }

    public String getDefaultSuffix()
    {
        return defaultSuffix;
    }

    /**
     * JavaMail session.
     */
    public Session createSession()
    {
        Properties props = new Properties( System.getProperties() );
        if ( smtpHost != null )
        {
            props.put( "mail.smtp.host", smtpHost );
        }
        if ( smtpPort != null )
        {
            props.put( "mail.smtp.port", smtpPort );
        }
        if ( useSsl )
        {
            /* This allows the user to override settings by setting system properties but
                    * also allows us to use the default SMTPs port of 465 if no port is already set.
                    * It would be cleaner to use smtps, but that's done by calling session.getTransport()...
                    * and thats done in mail sender, and it would be a bit of a hack to get it all to
                    * coordinate, and we can make it work through setting mail.smtp properties.
                    */
            if ( props.getProperty( "mail.smtp.socketFactory.port" ) == null )
            {
                String port = smtpPort == null ? "465" : smtpPort;
                props.put( "mail.smtp.port", port );
                props.put( "mail.smtp.socketFactory.port", port );
            }
            if ( props.getProperty( "mail.smtp.socketFactory.class" ) == null )
            {
                props.put( "mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory" );
            }
            props.put( "mail.smtp.socketFactory.fallback", "false" );
        }
        if ( smtpAuthUsername != null )
        {
            props.put( "mail.smtp.auth", "true" );
        }
        return Session.getInstance( props, getAuthenticator() );
    }

    private Authenticator getAuthenticator()
    {
        final String un = getSmtpAuthUsername();
        if ( un == null )
        {
            return null;
        }
        return new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication( getSmtpAuthUsername(), getSmtpAuthPassword() );
            }
        };
    }

    public String getHudsonUrl()
    {
        return hudsonUrl;
    }

    public String getSmtpServer()
    {
        return smtpHost;
    }

    public String getSmtpAuthUsername()
    {
        return smtpAuthUsername;
    }

    public String getSmtpAuthPassword()
    {
        return Secret.toString( smtpAuthPassword );
    }

    public boolean getUseSsl()
    {
        return useSsl;
    }

    public String getSmtpPort()
    {
        return smtpPort;
    }

    public String getDefaultContentType()
    {
        return defaultContentType;
    }

    public String getDefaultSubject()
    {
        return defaultSubject;
    }

    public String getDefaultBody()
    {
        return defaultBody;
    }

    public boolean getOverrideGlobalSettings()
    {
        return overrideGlobalSettings;
    }

    public boolean isApplicable( Class<? extends AbstractProject> jobType )
    {
        return true;
    }

    @Override
    public Publisher newInstance( StaplerRequest req, JSONObject formData )
        throws hudson.model.Descriptor.FormException
    {
        // Save the recipient lists
        String listRecipients = formData.getString( "recipientlist_recipients" );

        // Save configuration for each trigger type
        ExtendedEmailPublisher m = new ExtendedEmailPublisher();
        m.recipientList = listRecipients;
        m.contentType = formData.getString( "project_content_type" );
        m.defaultSubject = formData.getString( "project_default_subject" );
        m.defaultContent = formData.getString( "project_default_content" );
        m.configuredTriggers = new ArrayList<EmailTrigger>();

        // Create a new email trigger for each one that is configured
        for ( String mailerId : ExtendedEmailPublisher.EMAIL_TRIGGER_TYPE_MAP.keySet() )
        {
            if ( "true".equalsIgnoreCase( formData.optString( "mailer_" + mailerId + "_configured" ) ) )
            {
                EmailType type = createMailType( formData, mailerId );
                EmailTrigger trigger = ExtendedEmailPublisher.EMAIL_TRIGGER_TYPE_MAP.get( mailerId ).getNewInstance( type );
                m.configuredTriggers.add( trigger );
            }
        }

        return m;
    }

    private EmailType createMailType( JSONObject formData, String mailType )
    {
        EmailType m = new EmailType();
        String prefix = "mailer_" + mailType + '_';
        m.setSubject( formData.getString( prefix + "subject" ) );
        m.setBody( formData.getString( prefix + "body" ) );
        m.setRecipientList( formData.getString( prefix + "recipientList" ) );
        m.setSendToRecipientList( formData.optBoolean( prefix + "sendToRecipientList" ) );
        m.setSendToDevelopers( formData.optBoolean( prefix + "sendToDevelopers" ) );
        m.setIncludeCulprits( formData.optBoolean( prefix + "includeCulprits" ) );
        return m;
    }

    public ExtendedEmailPublisherDescriptor()
    {
        super( ExtendedEmailPublisher.class );
        load();
        if ( defaultBody == null && defaultSubject == null )
        {
            defaultBody = ExtendedEmailPublisher.DEFAULT_BODY_TEXT;
            defaultSubject = ExtendedEmailPublisher.DEFAULT_SUBJECT_TEXT;
        }
    }

    @Override
    public boolean configure( StaplerRequest req, JSONObject formData )
        throws FormException
    {
        // Most of this stuff is the same as the built-in email publisher

        // Configure the smtp server
        smtpHost = nullify( req.getParameter( "ext_mailer_smtp_server" ) );
        adminAddress = req.getParameter( "ext_mailer_admin_address" );
        defaultSuffix = nullify( req.getParameter( "ext_mailer_default_suffix" ) );

        // Specify the url to this hudson instance
        String url = nullify( req.getParameter( "ext_mailer_hudson_url" ) );
        if ( url != null && !url.endsWith( "/" ) )
        {
            url += '/';
        }
        if ( url == null )
        {
            url = Hudson.getInstance().getRootUrl();
        }
        hudsonUrl = url;

        // specify authentication information
        if ( req.getParameter( "extmailer.useSMTPAuth" ) != null )
        {
            smtpAuthUsername = nullify( req.getParameter( "extmailer.SMTPAuth.userName" ) );
            smtpAuthPassword = Secret.fromString( nullify( req.getParameter( "extmailer.SMTPAuth.password" ) ) );
        }
        else
        {
            smtpAuthUsername = null;
            smtpAuthPassword = null;
        }

        // specify if the mail server uses ssl for authentication
        useSsl = req.getParameter( "ext_mailer_smtp_use_ssl" ) != null;

        // specify custom smtp port
        smtpPort = nullify( req.getParameter( "ext_mailer_smtp_port" ) );

        defaultContentType = nullify( req.getParameter( "ext_mailer_default_content_type" ) );

        // Allow global defaults to be set for the subject and body of the email
        defaultSubject = nullify( req.getParameter( "ext_mailer_default_subject" ) );
        defaultBody = nullify( req.getParameter( "ext_mailer_default_body" ) );

        overrideGlobalSettings = req.getParameter( "ext_mailer_override_global_settings" ) != null;

        save();
        return super.configure( req, formData );
    }

    private String nullify( String v )
    {
        if ( v != null && v.length() == 0 )
        {
            v = null;
        }
        return v;
    }

    @Override
    public String getHelpFile()
    {
        return "/plugin/email-ext/help/main.html";
    }

    public FormValidation doAddressCheck( @QueryParameter final String value )
        throws IOException, ServletException
    {
        try
        {
            new InternetAddress( value );
            return FormValidation.ok();
        }
        catch ( AddressException e )
        {
            return FormValidation.error( e.getMessage() );
        }
    }

    public FormValidation doRecipientListRecipientsCheck( @QueryParameter final String value )
        throws IOException, ServletException
    {
        return new EmailRecepientUtils().validateFormRecipientList( value );
		}

	}
