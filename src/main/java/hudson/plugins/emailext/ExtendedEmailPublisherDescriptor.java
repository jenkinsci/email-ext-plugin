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
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * These settings are global configurations
 */
@Extension
public final class ExtendedEmailPublisherDescriptor extends BuildStepDescriptor<Publisher> {

    public static final Logger LOGGER = Logger.getLogger(ExtendedEmailPublisherDescriptor.class.getName());
    /**
     * The default e-mail address suffix appended to the user name found from
     * changelog, to send e-mails. Null if not configured.
     */
    private String defaultSuffix;

    /**
     * Jenkins's own URL, to put into the e-mail.
     */
    private transient String hudsonUrl;

    MailAccount mailAccount = new MailAccount();

    private List<MailAccount> addAccounts = new ArrayList<>();

    /**
     * The e-mail address that Jenkins puts to "From:" field in outgoing
     * e-mails. Null if not configured.
     */
    private transient String adminAddress;

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

    private List<GroovyScriptPath> defaultClasspath = new ArrayList<>();

    private transient List<EmailTriggerDescriptor> defaultTriggers = new ArrayList<>();

    private List<String> defaultTriggerIds = new ArrayList<>();

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

    /**
     * Enables the "Allow Unregistered Emails" feature
     */
    private boolean enableAllowUnregistered;

    public ExtendedEmailPublisherDescriptor() {
        super(ExtendedEmailPublisher.class);
        load();
        if (defaultBody == null && defaultSubject == null && emergencyReroute == null) {
            defaultBody = ExtendedEmailPublisher.DEFAULT_BODY_TEXT;
            defaultSubject = ExtendedEmailPublisher.DEFAULT_SUBJECT_TEXT;
            emergencyReroute = ExtendedEmailPublisher.DEFAULT_EMERGENCY_REROUTE_TEXT;
        }

        if (Jenkins.getActiveInstance().isUseSecurity()
                && (!StringUtils.isBlank(this.defaultPostsendScript)) || !StringUtils.isBlank(this.defaultPresendScript)) {
            setDefaultPostsendScript(this.defaultPostsendScript);
            setDefaultPresendScript(this.defaultPresendScript);
            try {
                setDefaultClasspath(this.defaultClasspath);
            } catch (FormException e) {
                //Some of the old configured classpaths probably used some environment variable, let's clean those out
                List<GroovyScriptPath> newList = new ArrayList<>();
                for (GroovyScriptPath path : defaultClasspath) {
                    URL u = path.asURL();
                    if (u != null) {
                        try {
                            new ClasspathEntry(u.toString());
                            newList.add(path);
                        } catch (MalformedURLException mfue) {
                            LOGGER.log(Level.WARNING, "The default classpath contained a malformed url, will be ignored.", mfue);
                        }
                    }
                }
                try {
                    setDefaultClasspath(newList);
                } catch (FormException e1) {
                    assert false : e1;
                }
            }
        }
    }

    @Override
    public String getDisplayName() {
        return Messages.ExtendedEmailPublisherDescriptor_DisplayName();
    }

    public String getAdminAddress() {
        JenkinsLocationConfiguration configuration = JenkinsLocationConfiguration.get();
        assert configuration != null;
        return configuration.getAdminAddress();
    }

    public String getDefaultSuffix() {
        return defaultSuffix;
    }

    public void setDefaultSuffix(String defaultSuffix) {
        this.defaultSuffix = defaultSuffix;
    }

    public Session createSession(String from) throws MessagingException {
        Properties props = new Properties(System.getProperties());

        MailAccount acc = mailAccount;
        if(StringUtils.isNotBlank(from)){
            InternetAddress fromAddress = new InternetAddress(from);
            for(MailAccount ma : addAccounts){
                if(!ma.getAddress().equalsIgnoreCase(fromAddress.getAddress())) continue;
                acc = ma;
                break;
            }
        }

        if (acc.getSmtp_host() != null) {
            props.put("mail.smtp.host", acc.getSmtp_host());
        }
        if (acc.getSmtp_port() != null) {
            props.put("mail.smtp.port", acc.getSmtp_port());
        }
        if (acc.isUse_ssl()) {
            /* This allows the user to override settings by setting system properties but
             * also allows us to use the default SMTPs port of 465 if no port is already set.
             * It would be cleaner to use smtps, but that's done by calling session.getTransport()...
             * and thats done in mail sender, and it would be a bit of a hack to get it all to
             * coordinate, and we can make it work through setting mail.smtp properties.
             */
            if (props.getProperty("mail.smtp.socketFactory.port") == null) {
                String port = acc.getSmtp_port() == null ? "465" : mailAccount.getSmtp_port();
                props.put("mail.smtp.port", port);
                props.put("mail.smtp.socketFactory.port", port);
            }
            if (props.getProperty("mail.smtp.socketFactory.class") == null) {
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }
            props.put("mail.smtp.socketFactory.fallback", "false");
        }
        if (acc.getSmtp_username() != null) {
            props.put("mail.smtp.auth", "true");
        }

        // avoid hang by setting some timeout.
        props.put("mail.smtp.timeout", "60000");
        props.put("mail.smtp.connectiontimeout", "60000");

        try {
            String ap = acc.getAdv_properties();
            if (ap != null && !isBlank(ap.trim())) {
                props.load(new StringReader(ap));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Parameters parse fail.", e);
        }

        return Session.getInstance(props, getAuthenticator(acc));
    }

    private Authenticator getAuthenticator(final MailAccount acc) {
        if (acc == null || acc.getSmtp_username() == null) {
            return null;
        }
        return new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(acc.getSmtp_username(), acc.getSmtp_password());
            }
        };
    }

    public String getHudsonUrl() {
        return Jenkins.getActiveInstance().getRootUrl();
    }

    public List<MailAccount> getAddAccounts() {
        return addAccounts;
    }

    public String getSmtpServer() {
        return mailAccount.getSmtp_host();
    }

    public void setSmtpServer(String smtpServer) {
        mailAccount.setSmtp_host(smtpServer);
    }

    public String getSmtpAuthUsername() {
        return mailAccount.getSmtp_username();
    }

    @SuppressWarnings("unused")
    public void setSmtpAuthUsername(String username) {
        mailAccount.setSmtp_username(username);
    }

    public String getSmtpAuthPassword() {
        return mailAccount.getSmtp_password();
    }

    @SuppressWarnings("unused")
    public void setSmtpAuthPassword(String password) {
        mailAccount.setSmtp_password(password);
    }

    // Make API match Mailer plugin
    @SuppressWarnings("unused")
    public void setSmtpAuth(String userName, String password) {
        setSmtpAuthUsername(userName);
        setSmtpAuthPassword(password);
    }

    public boolean getUseSsl() {
        return mailAccount.isUse_ssl();
    }

    @SuppressWarnings("unused")
    public void setUseSsl(boolean useSsl) {
        mailAccount.setUse_ssl(useSsl);
    }

    public String getSmtpPort() {
        return mailAccount.getSmtp_port();
    }

    @SuppressWarnings("unused")
    public void setSmtpPort(String port) {
        mailAccount.setSmtp_port(nullify(port));
    }

    public String getCharset() {
        String c = charset;
        if (StringUtils.isBlank(c)) {
            c = "UTF-8";
        }
        return c;
    }

    @SuppressWarnings("unused")
    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }

    @SuppressWarnings("unused")
    public void setDefaultContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            this.defaultContentType = "text/plain";
        } else {
            this.defaultContentType = contentType;
        }
    }

    public String getDefaultSubject() {
        return defaultSubject;
    }

    @SuppressWarnings("unused")
    public void setDefaultSubject(String subject) {
        if (subject == null) {
            this.defaultSubject = ExtendedEmailPublisher.DEFAULT_SUBJECT_TEXT;
        } else {
            this.defaultSubject = subject;
        }
    }

    public String getDefaultBody() {
        return defaultBody;
    }

    @SuppressWarnings("unused")
    public void setDefaultBody(String body) {
        if (body == null) {
            this.defaultBody = ExtendedEmailPublisher.DEFAULT_BODY_TEXT;
        } else {
            this.defaultBody = body;
        }
    }

    public String getEmergencyReroute() {
        return emergencyReroute;
    }

    protected void setEmergencyReroute(String emergencyReroute) {
        if (emergencyReroute == null) {
            this.emergencyReroute = ExtendedEmailPublisher.DEFAULT_EMERGENCY_REROUTE_TEXT;
        } else {
            this.emergencyReroute = emergencyReroute;
        }
    }

    public long getMaxAttachmentSize() {
        return maxAttachmentSize;
    }

    public void setMaxAttachmentSize(long bytes) {
        if (bytes < 0) {
            bytes = -1; // set to default "empty" value
        }
        this.maxAttachmentSize = bytes;
    }

    public long getMaxAttachmentSizeMb() {
        return maxAttachmentSize / (1024 * 1024);
    }

    @SuppressWarnings("unused")
    public void setMaxAttachmentSizeMb(long mb) {
        setMaxAttachmentSize(mb * (1024 * 1024));
    }

    public String getDefaultRecipients() {
        return recipientList;
    }

    @SuppressWarnings("unused")
    public void setDefaultRecipients(String recipients) {
        this.recipientList = ((recipients == null) ? "" : recipients);
    }

    public String getExcludedCommitters() {
        return excludedCommitters;
    }

    @SuppressWarnings("unused")
    public void setExcludedCommitters(String excluded) {
        this.excludedCommitters = ((excluded == null) ? "" : excluded);
    }

    public boolean getOverrideGlobalSettings() {
        return overrideGlobalSettings;
    }

    public String getListId() {
        return listId;
    }

    @SuppressWarnings("unused")
    public void setListId(String id) {
        this.listId = nullify(id);
    }

    public boolean getPrecedenceBulk() {
        return precedenceBulk;
    }

    @SuppressWarnings("unused")
    public void setPrecedenceBulk(boolean bulk) {
        this.precedenceBulk = bulk;
    }

    public String getDefaultReplyTo() {
        return defaultReplyTo;
    }

    @SuppressWarnings("unused")
    public void setDefaultReplyTo(String to) {
        this.defaultReplyTo = ((to == null) ? "" : to);
    }

    public boolean isSecurityEnabled() {
        return false;
    }

    public boolean isAdminRequiredForTemplateTesting() {
        return requireAdminForTemplateTesting;
    }

    @SuppressWarnings("unused")
    public void setAdminRequiredForTemplateTesting(boolean requireAdmin) {
        this.requireAdminForTemplateTesting = requireAdmin;
    }

    public boolean isWatchingEnabled() {
        return enableWatching;
    }

    public boolean isAllowUnregisteredEnabled() {
        return enableAllowUnregistered;
    }

    @SuppressWarnings("unused")
    public void setWatchingEnabled(boolean enabled) {
        this.enableWatching = enabled;
    }

    @SuppressWarnings("unused")
    public void setAllowUnregisteredEnabled(boolean enabled) {
        this.enableAllowUnregistered = enabled;
    }

    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    public String getDefaultPresendScript() {
        return defaultPresendScript;
    }

    @SuppressWarnings("unused")
    public void setDefaultPresendScript(String script) {
        script = StringUtils.trim(script);
        this.defaultPresendScript = ScriptApproval.get().configuring(((script == null) ? "" : script),
                GroovyLanguage.get(),
                ApprovalContext.create().withCurrentUser());
    }

    public String getDefaultPostsendScript() {
        return defaultPostsendScript;
    }

    @SuppressWarnings("unused")
    public void setDefaultPostsendScript(String script) {
        script = StringUtils.trim(script);
        this.defaultPostsendScript = ScriptApproval.get().configuring(((script == null) ? "" : script),
                GroovyLanguage.get(),
                ApprovalContext.create().withCurrentUser());
    }

    public List<GroovyScriptPath> getDefaultClasspath() {
        return defaultClasspath;
    }

    public void setDefaultClasspath(List<GroovyScriptPath> defaultClasspath) throws FormException {
        if (Jenkins.getActiveInstance().isUseSecurity()) {
            ScriptApproval approval = ScriptApproval.get();
            ApprovalContext context = ApprovalContext.create().withCurrentUser();
            for (GroovyScriptPath path : defaultClasspath) {
                URL u = path.asURL();
                if (u != null) {
                    try {
                        approval.configuring(new ClasspathEntry(u.toString()), context);
                    } catch (MalformedURLException e) {
                        throw new FormException(e, "ext_mailer_default_classpath");
                    }
                }
            }
        }
        this.defaultClasspath = defaultClasspath;
    }

    public List<String> getDefaultTriggerIds() {
        if (defaultTriggerIds.isEmpty()) {
            if (!defaultTriggers.isEmpty()) {
                defaultTriggerIds.clear();
                for (EmailTriggerDescriptor t : this.defaultTriggers) {
                    // we have to do the below because a bunch of stuff is not serialized for the Descriptor
                    EmailTriggerDescriptor d = Jenkins.getActiveInstance().getDescriptorByType(t.getClass());
                    if (d != null && !defaultTriggerIds.contains(d.getId())) {
                        defaultTriggerIds.add(d.getId());
                    }
                }
            } else {
                FailureTrigger.DescriptorImpl f = Jenkins.getActiveInstance().getDescriptorByType(FailureTrigger.DescriptorImpl.class);
                if (f != null) {
                    defaultTriggerIds.add(f.getId());
                }
            }
            save();
        }
        return defaultTriggerIds;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws FormException {

        // Configure the smtp server
        mailAccount.setSmtp_host(nullify(req.getParameter("ext_mailer_smtp_server")));
        defaultSuffix = nullify(req.getParameter("ext_mailer_default_suffix"));

        mailAccount.setAdv_properties(nullify(req.getParameter("ext_mailer_adv_properties")));

        addAccounts.clear();
        Object addacc = formData.opt("addAccounts");
        if(addacc != null){
            if(addacc instanceof JSONArray){
                for(Object obj : (JSONArray)addacc) addAccounts.add(new MailAccount((JSONObject)obj));
            }
            else if(addacc instanceof JSONObject){
                addAccounts.add(new MailAccount((JSONObject)addacc));
            }
        }

        // specify authentication information
        if (req.hasParameter("ext_mailer_use_smtp_auth")) {
            mailAccount.setSmtp_username(nullify(req.getParameter("ext_mailer_smtp_username")));
            mailAccount.setSmtp_password(nullify(req.getParameter("ext_mailer_smtp_password")));
        } else {
            mailAccount.setSmtp_username(null);
            mailAccount.setSmtp_password(null);
        }

        // specify if the mail server uses ssl for authentication
        mailAccount.setUse_ssl(req.hasParameter("ext_mailer_smtp_use_ssl"));

        // specify custom smtp port
        mailAccount.setSmtp_port(nullify(req.getParameter("ext_mailer_smtp_port")));

        charset = nullify(req.getParameter("ext_mailer_charset"));

        defaultContentType = nullify(req.getParameter("ext_mailer_default_content_type"));

        // Allow global defaults to be set for the subject and body of the email
        defaultSubject = nullify(req.getParameter("ext_mailer_default_subject"));
        defaultBody = nullify(req.getParameter("ext_mailer_default_body"));
        emergencyReroute = nullify(req.getParameter("ext_mailer_emergency_reroute"));
        defaultReplyTo = nullify(req.getParameter("ext_mailer_default_replyto")) != null
                ? req.getParameter("ext_mailer_default_replyto") : "";
        setDefaultPresendScript(nullify(req.getParameter("ext_mailer_default_presend_script")) != null
                ? req.getParameter("ext_mailer_default_presend_script") : "");
        setDefaultPostsendScript(nullify(req.getParameter("ext_mailer_default_postsend_script")) != null
                ? req.getParameter("ext_mailer_default_postsend_script") : "");
        if (req.hasParameter("ext_mailer_default_classpath")) {
            List<GroovyScriptPath> cp = new ArrayList<>();
            for (String s : req.getParameterValues("ext_mailer_default_classpath")) {
                cp.add(new GroovyScriptPath(s));
            }
            setDefaultClasspath(cp);
        }
        debugMode = req.hasParameter("ext_mailer_debug_mode");

        // convert the value into megabytes (1024 * 1024 bytes)
        maxAttachmentSize = nullify(req.getParameter("ext_mailer_max_attachment_size")) != null
                ? Long.parseLong(req.getParameter("ext_mailer_max_attachment_size")) * 1024 * 1024 : -1;
        recipientList = nullify(req.getParameter("ext_mailer_default_recipients")) != null
                ? req.getParameter("ext_mailer_default_recipients") : "";

        precedenceBulk = req.hasParameter("ext_mailer_add_precedence_bulk");

        excludedCommitters = req.getParameter("ext_mailer_excluded_committers");

        requireAdminForTemplateTesting = req.hasParameter("ext_mailer_require_admin_for_template_testing");

        enableWatching = req.hasParameter("ext_mailer_watching_enabled");

        enableAllowUnregistered = req.hasParameter("ext_mailer_allow_unregistered_enabled");

        // specify List-ID information
        if (req.hasParameter("ext_mailer_use_list_id")) {
            listId = nullify(req.getParameter("ext_mailer_list_id"));
        } else {
            listId = null;
        }

        List<String> ids = new ArrayList<>();
        if (formData.optJSONArray("defaultTriggers") != null) {
            for (Object id : formData.getJSONArray("defaultTriggers")) {
                ids.add(id.toString());
            }
        } else if (StringUtils.isNotEmpty(formData.optString("defaultTriggers"))) {
            ids.add(formData.getString("defaultTriggers"));
        }

        if (!ids.isEmpty()) {
            defaultTriggerIds.clear();
            for (String id : ids) {
                EmailTriggerDescriptor d = (EmailTriggerDescriptor) Jenkins.getActiveInstance().getDescriptor(id);
                if (d != null) {
                    defaultTriggerIds.add(id);
                }
            }
        }

        if (!overrideGlobalSettings) {
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
        mailAccount.setUse_ssl(Mailer.descriptor().getUseSsl());
        if (StringUtils.isNotBlank(Mailer.descriptor().getSmtpAuthUserName())) {
            mailAccount.setSmtp_username(Mailer.descriptor().getSmtpAuthUserName());
            mailAccount.setSmtp_password(Mailer.descriptor().getSmtpAuthPassword());
        }
        mailAccount.setSmtp_host(Mailer.descriptor().getSmtpServer());
        mailAccount.setSmtp_port(Mailer.descriptor().getSmtpPort());
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
