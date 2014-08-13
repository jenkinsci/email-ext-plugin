package hudson.plugins.emailext;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
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
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * These settings are global configurations
 */
@Extension
public final class ExtendedEmailPublisherDescriptor extends BuildStepDescriptor<Publisher> {

    /**
     * The default e-mail address suffix appended to the user name found from changelog,
     * to send e-mails. Null if not configured.
     */
    private String defaultSuffix;

    /**
     * Jenkins's own URL, to put into the e-mail.
     */
    private transient String hudsonUrl;
 
    /**
     * If non-null, use SMTP-AUTH
     */
    private String smtpAuthUsername;

    private Secret smtpAuthPassword;

    /**
     * The e-mail address that Jenkins puts to "From:" field in outgoing e-mails.
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

    private String charset;

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
    
    /**
     * This is the global default pre-send script.
     */
    private String defaultPresendScript = "";
    
    private List<GroovyScriptPath> defaultClasspath = new ArrayList<GroovyScriptPath>();
    
    /**
     * This is the global emergency email address
     */
    private String emergencyReroute;
    
    /**
     * The maximum size of all the attachments (in bytes)
     */
    private long maxAttachmentSize = -1;
    
    /*
     * This is a global default recipient list for sending emails.
     */
    private String recipientList = "";

    /*
     * The default Reply-To header value
     */
    private String defaultReplyTo = "";

    /*
     * This is a global excluded committers list for not sending commit emails.
     */
    private String excludedCommitters = "";


    private boolean overrideGlobalSettings;
    
    /**
     * If non-null, set a List-ID email header.
     */
    private String listId;

    private boolean precedenceBulk;

    private boolean debugMode = false;

    private boolean enableSecurity = false;
    
    /**
     * Enables the "Watch This Job" feature
     */
    private boolean enableWatching;

    @Override
    public String getDisplayName() {
        return Messages.ExtendedEmailPublisherDescriptor_DisplayName();
    }

    public String getAdminAddress() {
        String v = adminAddress;
        if (v == null) {
            v = Messages.ExtendedEmailPublisherDescriptor_AdminAddress();
        }
        return v;
    }

    public String getDefaultSuffix() {
        return defaultSuffix;
    }
    
    /**
     * JavaMail session.
     */
    public Session createSession() {
        Properties props = new Properties(System.getProperties());
        if (smtpHost != null) {
            props.put("mail.smtp.host", smtpHost);
        }
        if (smtpPort != null) {
            props.put("mail.smtp.port", smtpPort);
        }
        if (useSsl) {
            /* This allows the user to override settings by setting system properties but
             * also allows us to use the default SMTPs port of 465 if no port is already set.
             * It would be cleaner to use smtps, but that's done by calling session.getTransport()...
             * and thats done in mail sender, and it would be a bit of a hack to get it all to
             * coordinate, and we can make it work through setting mail.smtp properties.
             */
            if (props.getProperty("mail.smtp.socketFactory.port") == null) {
                String port = smtpPort == null ? "465" : smtpPort;
                props.put("mail.smtp.port", port);
                props.put("mail.smtp.socketFactory.port", port);
            }
            if (props.getProperty("mail.smtp.socketFactory.class") == null) {
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }
            props.put("mail.smtp.socketFactory.fallback", "false");
        }
        if (smtpAuthUsername != null) {
            props.put("mail.smtp.auth", "true");
        }

        // avoid hang by setting some timeout.
        props.put("mail.smtp.timeout","60000");
        props.put("mail.smtp.connectiontimeout","60000");

        return Session.getInstance(props, getAuthenticator());
    }

    private Authenticator getAuthenticator() {
        final String un = getSmtpAuthUsername();
        if (un == null) {
            return null;
        }
        return new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getSmtpAuthUsername(), getSmtpAuthPassword());
            }
        };
    }

    public String getHudsonUrl() {
        return Jenkins.getInstance().getRootUrl();
    }

    public String getSmtpServer() {
        return smtpHost;
    }

    public String getSmtpAuthUsername() {
        return smtpAuthUsername;
    }

    public String getSmtpAuthPassword() {
        return Secret.toString(smtpAuthPassword);
    }

    public boolean getUseSsl() {
        return useSsl;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public String getCharset() {
        return charset;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }

    public String getDefaultSubject() {
        return defaultSubject;
    }

    public String getDefaultBody() {
        return defaultBody;
    }
    
    public String getEmergencyReroute() {
        return emergencyReroute;
    }

    protected void setEmergencyReroute(String emergencyReroute) {
        this.emergencyReroute = emergencyReroute;
    }
    
    public long getMaxAttachmentSize() {
        return maxAttachmentSize;
    }
    
    public long getMaxAttachmentSizeMb() {
        return maxAttachmentSize / (1024 * 1024);
    }
    
    public String getDefaultRecipients() {
        return recipientList;
    }

    public String getExcludedCommitters() {
        return excludedCommitters;
    }

    public boolean getOverrideGlobalSettings() {
        return overrideGlobalSettings;
    }

    public String getListId() {
        return listId;
    }

    public boolean getPrecedenceBulk() {
        return precedenceBulk;
    }

    public String getDefaultReplyTo() {
        return defaultReplyTo;
    }

    public boolean isSecurityEnabled() {
        return enableSecurity;
    }
    
    public boolean isWatchingEnabled() {
        return enableWatching;
    }

    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }
    
    public String getDefaultPresendScript() {
        return defaultPresendScript;
    }

    public List<GroovyScriptPath> getDefaultClasspath() {
        return defaultClasspath;
    }
    
    public ExtendedEmailPublisherDescriptor() {
        super(ExtendedEmailPublisher.class);
        load();
        if (defaultBody == null && defaultSubject == null && emergencyReroute == null) {
            defaultBody = ExtendedEmailPublisher.DEFAULT_BODY_TEXT;
            defaultSubject = ExtendedEmailPublisher.DEFAULT_SUBJECT_TEXT;
            emergencyReroute = ExtendedEmailPublisher.DEFAULT_EMERGENCY_REROUTE_TEXT;
            enableSecurity = false;
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws FormException {
        // Most of this stuff is the same as the built-in email publisher

        overrideGlobalSettings = req.hasParameter("ext_mailer_override_global_settings");

        // Configure the smtp server
        smtpHost = nullify(req.getParameter("ext_mailer_smtp_server"));
        adminAddress = req.getParameter("ext_mailer_admin_address");
        defaultSuffix = nullify(req.getParameter("ext_mailer_default_suffix"));
        
        // specify authentication information
        if (req.hasParameter("ext_mailer_use_smtp_auth")) {
            smtpAuthUsername = nullify(req.getParameter("ext_mailer_smtp_username"));
            smtpAuthPassword = Secret.fromString(nullify(req.getParameter("ext_mailer_smtp_password")));
        } else {
            smtpAuthUsername = null;
            smtpAuthPassword = null;
        }

        // specify if the mail server uses ssl for authentication
        useSsl = req.hasParameter("ext_mailer_smtp_use_ssl");

        // specify custom smtp port
        smtpPort = nullify(req.getParameter("ext_mailer_smtp_port"));

        charset = nullify(req.getParameter("ext_mailer_charset"));

        defaultContentType = nullify(req.getParameter("ext_mailer_default_content_type"));

        // Allow global defaults to be set for the subject and body of the email
        defaultSubject = nullify(req.getParameter("ext_mailer_default_subject"));
        defaultBody = nullify(req.getParameter("ext_mailer_default_body"));
        emergencyReroute = nullify(req.getParameter("ext_mailer_emergency_reroute"));
        defaultReplyTo = nullify(req.getParameter("ext_mailer_default_replyto")) != null ?
            req.getParameter("ext_mailer_default_replyto") : "";
        defaultPresendScript = nullify(req.getParameter("ext_mailer_default_presend_script")) != null ?
            req.getParameter("ext_mailer_default_presend_script") : "";
        if (req.hasParameter("ext_mailer_default_classpath")) {
            defaultClasspath.clear();
            for(String s : req.getParameterValues("ext_mailer_default_classpath")) {
                defaultClasspath.add(new GroovyScriptPath(s));
            }
        }
        debugMode = req.hasParameter("ext_mailer_debug_mode");
        
        //enableWatching = req.getParameter("ext_mailer_enable_watching") != null;

        // convert the value into megabytes (1024 * 1024 bytes)
        maxAttachmentSize = nullify(req.getParameter("ext_mailer_max_attachment_size")) != null ?
            (Long.parseLong(req.getParameter("ext_mailer_max_attachment_size")) * 1024 * 1024) : -1;
        recipientList = nullify(req.getParameter("ext_mailer_default_recipients")) != null ?
            req.getParameter("ext_mailer_default_recipients") : "";

        precedenceBulk = req.hasParameter("ext_mailer_add_precedence_bulk");
        enableSecurity = req.hasParameter("ext_mailer_security_enabled");

        excludedCommitters = req.getParameter("ext_mailer_excluded_committers");

        // specify List-ID information
        if (req.hasParameter("ext_mailer_use_list_id")) {
            listId = nullify(req.getParameter("ext_mailer_list_id"));
        } else {
            listId = null;
        }
        
        save();
        return super.configure(req, formData);
    }

    private String nullify(String v) {
        if (v != null && v.length() == 0) {
            v = null;
        }
        return v;
    }

    @Override
    public String getHelpFile() {
        return "/plugin/email-ext/help/main.html";
    }
    
    public FormValidation doAddressCheck(@QueryParameter final String value)
            throws IOException, ServletException {
        try {
            new InternetAddress(value);
            return FormValidation.ok();
        } catch (AddressException e) {
            return FormValidation.error(e.getMessage());
        }
    }
    
    public FormValidation doRecipientListRecipientsCheck(@QueryParameter final String value)
            throws IOException, ServletException {
        return new EmailRecipientUtils().validateFormRecipientList(value);
    }
    
    public FormValidation doMaxAttachmentSizeCheck(@QueryParameter final String value)
            throws IOException, ServletException {
        try {
            String testValue = value.trim();
            // we support an empty value (which means default)
            // or a number
            if(testValue.length() > 0) {
                Long.parseLong(testValue);
            }
            return FormValidation.ok();
        } catch (Exception e) {
            return FormValidation.error(e.getMessage());
        }
    }

    public boolean isMatrixProject(Object project) {
        return project instanceof MatrixProject;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void debug(PrintStream logger, String format, Object... args) {
        if(debugMode) {
            logger.format(format, args);
            logger.println();
        }
    }
}
