package hudson.plugins.emailext;

import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.VersionNumber;
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
import java.util.ArrayList;
import java.util.Properties;

/**
 * These settings are global configurations
 */
public class ExtendedEmailPublisherDescriptor extends BuildStepDescriptor<Publisher> {

    /**
     * The default e-mail address suffix appended to the user name found from changelog,
     * to send e-mails. Null if not configured.
     */
    private String defaultSuffix;

    /**
     * Jenkins's own URL, to put into the e-mail.
     */
    private String hudsonUrl;
 
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

    private boolean overrideGlobalSettings;
    
    /**
     * If non-null, set a List-ID email header.
     */
    private String listId;

    private boolean precedenceBulk;

    private boolean debugMode;

    private boolean enableSecurity;

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
        return hudsonUrl;
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
    
    public long getMaxAttachmentSize() {
    	return maxAttachmentSize;
    }
    
    public long getMaxAttachmentSizeMb() {
        return maxAttachmentSize / (1024 * 1024);
    }
    
    public String getDefaultRecipients() {
        return recipientList;
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

    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }
    
    public boolean isTokenMacroAvailable() {
        boolean result = false;
        Plugin tokenMacroPlugin = Jenkins.getInstance().getPlugin("token-macro");
        if(tokenMacroPlugin != null) {
            result = !tokenMacroPlugin.getWrapper().getVersionNumber().isOlderThan(new VersionNumber("1.5.1"));
        }
        return result;
    }

    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData)
            throws hudson.model.Descriptor.FormException {
        // Save configuration for each trigger type
        ExtendedEmailPublisher m = new ExtendedEmailPublisher();
        m.recipientList = formData.getString("recipientlist_recipients");
        m.contentType = formData.getString("project_content_type");
        m.defaultSubject = formData.getString("project_default_subject");
        m.defaultContent = formData.getString("project_default_content");
        m.attachmentsPattern = formData.getString("project_attachments");
        m.presendScript = formData.getString("project_presend_script");
        m.attachBuildLog = "true".equalsIgnoreCase(formData.optString("project_attach_buildlog"));
        m.compressBuildLog = "true".equalsIgnoreCase(formData.optString("project_compress_buildlog"));
        m.replyTo = formData.getString("project_replyto");
        m.configuredTriggers = new ArrayList<EmailTrigger>();

        // Create a new email trigger for each one that is configured
        for (String mailerId : ExtendedEmailPublisher.EMAIL_TRIGGER_TYPE_MAP.keySet()) {
            if ("true".equalsIgnoreCase(formData.optString("mailer_" + mailerId + "_configured"))) {
                EmailType type = createMailType(formData, mailerId);
                EmailTrigger trigger = ExtendedEmailPublisher.EMAIL_TRIGGER_TYPE_MAP.get(mailerId).getNewInstance(type, req, formData);
                m.configuredTriggers.add(trigger);
            }
        }

        m.setMatrixTriggerMode(req.bindJSON(MatrixTriggerMode.class,MatrixTriggerMode.class,formData.opt("matrixTriggerMode")));

        return m;
    }

    private EmailType createMailType(JSONObject formData, String mailType) {
        EmailType m = new EmailType();
        String prefix = "mailer_" + mailType + '_';
        m.setSubject(formData.getString(prefix + "subject"));
        m.setBody(formData.getString(prefix + "body"));
        m.setRecipientList(formData.getString(prefix + "recipientList"));
        m.setSendToRecipientList(formData.optBoolean(prefix + "sendToRecipientList"));
        m.setSendToDevelopers(formData.optBoolean(prefix + "sendToDevelopers"));
        m.setSendToRequester(formData.optBoolean(prefix + "sendToRequester"));
        m.setIncludeCulprits(formData.optBoolean(prefix + "includeCulprits"));
        m.setAttachmentsPattern(formData.getString(prefix + "attachmentsPattern"));
        m.setAttachBuildLog(formData.optBoolean(prefix + "attachBuildLog"));
        m.setCompressBuildLog(formData.optBoolean(prefix + "compressBuildLog"));
        m.setReplyTo(formData.getString(prefix + "replyTo"));
        return m;
    }

    public ExtendedEmailPublisherDescriptor() {
        super(ExtendedEmailPublisher.class);
        load();
        if (defaultBody == null && defaultSubject == null && emergencyReroute == null) {
            defaultBody = ExtendedEmailPublisher.DEFAULT_BODY_TEXT;
            defaultSubject = ExtendedEmailPublisher.DEFAULT_SUBJECT_TEXT;
            emergencyReroute = ExtendedEmailPublisher.DEFAULT_EMERGENCY_REROUTE_TEXT;
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws FormException {
        // Most of this stuff is the same as the built-in email publisher

        overrideGlobalSettings = req.getParameter("ext_mailer_override_global_settings") != null;

        // Configure the smtp server
        smtpHost = nullify(req.getParameter("ext_mailer_smtp_server"));
        adminAddress = req.getParameter("ext_mailer_admin_address");
        defaultSuffix = nullify(req.getParameter("ext_mailer_default_suffix"));
        
        // Specify the url to this Jenkins instance
        String url = nullify(req.getParameter("ext_mailer_hudson_url"));
        if (url != null && !url.endsWith("/")) {
            url += '/';
        }
        if (!overrideGlobalSettings || url == null) {
            url = Jenkins.getInstance().getRootUrl();
        }
        hudsonUrl = url;

        // specify authentication information
        if (req.getParameter("extmailer.useSMTPAuth") != null) {
            smtpAuthUsername = nullify(req.getParameter("extmailer.SMTPAuth.userName"));
            smtpAuthPassword = Secret.fromString(nullify(req.getParameter("extmailer.SMTPAuth.password")));
        } else {
            smtpAuthUsername = null;
            smtpAuthPassword = null;
        }

        // specify if the mail server uses ssl for authentication
        useSsl = req.getParameter("ext_mailer_smtp_use_ssl") != null;

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

        debugMode = req.getParameter("ext_mailer_debug_mode") != null;

        // convert the value into megabytes (1024 * 1024 bytes)
        maxAttachmentSize = nullify(req.getParameter("ext_mailer_max_attachment_size")) != null ?
            (Long.parseLong(req.getParameter("ext_mailer_max_attachment_size")) * 1024 * 1024) : -1;
        recipientList = nullify(req.getParameter("ext_mailer_default_recipients")) != null ?
            req.getParameter("ext_mailer_default_recipients") : "";

        precedenceBulk = req.getParameter("extmailer.addPrecedenceBulk") != null;
        enableSecurity = req.getParameter("ext_mailer_security_enabled") != null;

        // specify List-ID information
        if (req.getParameter("extmailer.useListID") != null) {
            listId = nullify(req.getParameter("extmailer.ListID.id"));
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

    public boolean isMatrixProject(AbstractProject<?, ?> project) {
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
