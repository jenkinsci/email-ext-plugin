package hudson.plugins.emailext;

import hudson.Extension;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.trigger.FailureTrigger;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Mailer;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * These settings are global configurations
 */
@Extension
public final class ExtendedEmailPublisherDescriptor extends BuildStepDescriptor<Publisher> {

    /**
     * The default e-mail address suffix appended to the user name found from
     * changelog, to send e-mails. Null if not configured.
     */
    private String defaultSuffix;

    /**
     * Jenkins's own URL, to put into the e-mail.
     */
    private transient String hudsonUrl;

    /**
     * o
     * If non-null, use SMTP-AUTH
     */
    private String smtpAuthUsername;

    private Secret smtpAuthPassword;

    /**
     * The e-mail address that Jenkins puts to "From:" field in outgoing
     * e-mails. Null if not configured.
     */
    private transient String adminAddress;

    /**
     * The SMTP server to use for sending e-mail. Null for default to the
     * environment, which is usually <tt>localhost</tt>.
     */
    private String smtpHost;

    /**
     * If true use SSL on port 465 (standard SMTPS) unless <code>smtpPort</code>
     * is set.
     */
    private boolean useSsl;

    /**
     * The SMTP port to use for sending e-mail. Null for default to the
     * environment, which is usually <tt>25</tt>.
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

    /**
     * This is the global default post-send script.
     */
    private String defaultPostsendScript = "";

    private List<GroovyScriptPath> defaultClasspath = new ArrayList<GroovyScriptPath>();
    
    private transient List<EmailTriggerDescriptor> defaultTriggers = new ArrayList<EmailTriggerDescriptor>();
    
    private List<String> defaultTriggerIds = new ArrayList<String>();
    
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

    private transient boolean enableSecurity = false;

    /**
     * If true, then the 'Email Template Testing' link will only be displayed
     * for users with ADMINISTER privileges.
     */
    private boolean requireAdminForTemplateTesting = false;

    /**
     * Enables the "Watch This Job" feature
     */
    private boolean enableWatching;

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
    public String getDisplayName() {
        return Messages.ExtendedEmailPublisherDescriptor_DisplayName();
    }

    public String getAdminAddress() {
        return JenkinsLocationConfiguration.get().getAdminAddress();
    }

    public String getDefaultSuffix() {
        return defaultSuffix;
    }
    
    public void setDefaultSuffix(String defaultSuffix) {
        this.defaultSuffix = defaultSuffix;
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
        props.put("mail.smtp.timeout", "60000");
        props.put("mail.smtp.connectiontimeout", "60000");

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
        return Jenkins.getActiveInstance().getRootUrl();
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
        String c = charset;
        if (StringUtils.isBlank(c)) {
            c = "UTF-8";
        }
        return c;
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
        return false;
    }

    public boolean isAdminRequiredForTemplateTesting() {
        return requireAdminForTemplateTesting;
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

    public String getDefaultPostsendScript() {
        return defaultPostsendScript;
    }

    public List<GroovyScriptPath> getDefaultClasspath() {
        return defaultClasspath;
    }
    
    public List<String> getDefaultTriggerIds() {
        if (defaultTriggerIds.isEmpty()) {
            if (!defaultTriggers.isEmpty()) {
                defaultTriggerIds.clear();
                for(EmailTriggerDescriptor t : this.defaultTriggers) {
                    // we have to do the below because a bunch of stuff is not serialized for the Descriptor
                    EmailTriggerDescriptor d = (EmailTriggerDescriptor)Jenkins.getActiveInstance().getDescriptorByType(t.getClass());
                    if(!defaultTriggerIds.contains(d.getId())) {
                        defaultTriggerIds.add(d.getId());
                    }
                }
            } else {
                defaultTriggerIds.add(Jenkins.getActiveInstance().getDescriptor(FailureTrigger.class).getId());
            }
            save();
        }
        return defaultTriggerIds;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws FormException {

        // Configure the smtp server
        smtpHost = nullify(req.getParameter("ext_mailer_smtp_server"));
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
        defaultReplyTo = nullify(req.getParameter("ext_mailer_default_replyto")) != null
                ? req.getParameter("ext_mailer_default_replyto") : "";
        defaultPresendScript = nullify(req.getParameter("ext_mailer_default_presend_script")) != null
                ? req.getParameter("ext_mailer_default_presend_script") : "";
        defaultPostsendScript = nullify(req.getParameter("ext_mailer_default_postsend_script")) != null
                ? req.getParameter("ext_mailer_default_postsend_script") : "";
        if (req.hasParameter("ext_mailer_default_classpath")) {
            defaultClasspath.clear();
            for (String s : req.getParameterValues("ext_mailer_default_classpath")) {
                defaultClasspath.add(new GroovyScriptPath(s));
            }
        }
        debugMode = req.hasParameter("ext_mailer_debug_mode");

        // convert the value into megabytes (1024 * 1024 bytes)
        maxAttachmentSize = nullify(req.getParameter("ext_mailer_max_attachment_size")) != null
                ? (Long.parseLong(req.getParameter("ext_mailer_max_attachment_size")) * 1024 * 1024) : -1;
        recipientList = nullify(req.getParameter("ext_mailer_default_recipients")) != null
                ? req.getParameter("ext_mailer_default_recipients") : "";

        precedenceBulk = req.hasParameter("ext_mailer_add_precedence_bulk");

        excludedCommitters = req.getParameter("ext_mailer_excluded_committers");

        requireAdminForTemplateTesting = req.hasParameter("ext_mailer_require_admin_for_template_testing");

        enableWatching = req.hasParameter("ext_mailer_watching_enabled");

        // specify List-ID information
        if (req.hasParameter("ext_mailer_use_list_id")) {
            listId = nullify(req.getParameter("ext_mailer_list_id"));
        } else {
            listId = null;
        }

        List<String> ids = new ArrayList<String>();
        if(formData.optJSONArray("defaultTriggers") != null) {
            for(Object id : formData.getJSONArray("defaultTriggers")) {
               ids.add(id.toString());
            }
        } else if(StringUtils.isNotEmpty(formData.optString("defaultTriggers"))) {
            ids.add(formData.getString("defaultTriggers"));
        }
        
        if(!ids.isEmpty()) {
            defaultTriggerIds.clear();
            for(String id : ids) {
               EmailTriggerDescriptor d = (EmailTriggerDescriptor)Jenkins.getActiveInstance().getDescriptor(id);
               if(d != null) {
                   defaultTriggerIds.add(id);
               }
            }
        }
        
        if(!overrideGlobalSettings) {
            upgradeFromMailer();
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

    void upgradeFromMailer() {
        // get the data from Mailer and then set override to true
        this.defaultSuffix = Mailer.descriptor().getDefaultSuffix();
        this.defaultReplyTo = Mailer.descriptor().getReplyToAddress();
        this.useSsl = Mailer.descriptor().getUseSsl();
        if (StringUtils.isNotBlank(Mailer.descriptor().getSmtpAuthUserName())) {
            this.smtpAuthPassword = Secret.fromString(Mailer.descriptor().getSmtpAuthPassword());
            this.smtpAuthUsername = Mailer.descriptor().getSmtpAuthUserName();
        }
        this.smtpPort = Mailer.descriptor().getSmtpPort();
        this.smtpHost = Mailer.descriptor().getSmtpServer();
        this.charset = Mailer.descriptor().getCharset();
        this.overrideGlobalSettings = true;
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
            if (testValue.length() > 0) {
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
        if (debugMode) {
            logger.format(format, args);
            logger.println();
        }
    }
}
