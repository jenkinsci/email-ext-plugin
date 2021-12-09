package hudson.plugins.emailext;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnamePortRequirement;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.trigger.FailureTrigger;
import hudson.security.Permission;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * These settings are global configurations
 */
@Extension
@Symbol({"email-ext", "extendedEmailPublisher"})
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

    private MailAccount mailAccount = new MailAccount();

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
     * This is a global list of domains where we can send emails to
     */
    private String allowedDomains = null;

    /*
     * This is a global excluded committers list for not sending commit emails.
     */
    private String excludedCommitters = "";

    private transient boolean overrideGlobalSettings;

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

    private transient String smtpHost;
    private transient String smtpPort;
    private transient String smtpAuthUsername;
    private transient Secret smtpAuthPassword;
    private transient boolean useSsl = false;

    private transient BiFunction<MailAccount, Run<?,?>, Authenticator> authenticatorProvider = (acc, run) -> new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            DomainRequirement domainRequirement = null;
            if(StringUtils.isNotBlank(acc.getSmtpHost()) && StringUtils.isNotBlank(acc.getSmtpPort())) {
                domainRequirement = new HostnamePortRequirement(acc.getSmtpHost(), Integer.parseInt(acc.getSmtpPort()));
            }

            StandardUsernamePasswordCredentials c = CredentialsProvider.findCredentialById(acc.getCredentialsId(),
                            StandardUsernamePasswordCredentials.class,
                            run,
                            domainRequirement
                            );

            if(c == null) {
                return null;
            }

            return new PasswordAuthentication(c.getUsername(), Secret.toString(c.getPassword()));
        }
    };

    private Object readResolve(){
        if(smtpHost != null) mailAccount.setSmtpHost(smtpHost);
        if(smtpPort != null) mailAccount.setSmtpPort(smtpPort);
        if(smtpAuthUsername != null) mailAccount.setSmtpUsername(smtpAuthUsername);
        if(smtpAuthPassword != null) mailAccount.setSmtpPassword(smtpAuthPassword);
        if(useSsl) mailAccount.setUseSsl(useSsl);

        /*
         * Versions 2.71 and earlier correctly left the address unset for the default account,
         * relying solely on the system admin email address from the Jenkins Location settings for
         * the default account and using the address specified on the account only for additional
         * accounts. Versions 2.72 through 2.77 incorrectly set the address for the default account
         * to the system admin email address from the Jenkins Location settings at the time the
         * descriptor was first saved without propagating further changes from the Jenkins Location
         * settings to the default account. To clear up this bad state, we unconditionally clear the
         * address and rely once again solely on the system admin email address from the Jenkins
         * Location settings for the default account.
         */
        if (mailAccount.getAddress() != null) {
            mailAccount.setAddress(null);
        }

        return this;
    }

    public ExtendedEmailPublisherDescriptor() {
        super(ExtendedEmailPublisher.class);
        load();

        if (defaultBody == null && defaultSubject == null && emergencyReroute == null) {
            defaultBody = ExtendedEmailPublisher.DEFAULT_BODY_TEXT;
            defaultSubject = ExtendedEmailPublisher.DEFAULT_SUBJECT_TEXT;
            emergencyReroute = ExtendedEmailPublisher.DEFAULT_EMERGENCY_REROUTE_TEXT;
        }

        if(mailAccount == null) {
            mailAccount = new MailAccount();
        }

        mailAccount.setDefaultAccount(true);
    }

    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED, before = InitMilestone.JOB_LOADED)
    public static void autoConfigure() {
        ExtendedEmailPublisherDescriptor descriptor = ExtendedEmailPublisher.descriptor();

        if (Jenkins.get().isUseSecurity()
                && (!StringUtils.isBlank(descriptor.getDefaultPostsendScript())) || !StringUtils.isBlank(descriptor.getDefaultPresendScript())) {
            descriptor.setDefaultPostsendScript(descriptor.getDefaultPostsendScript());
            descriptor.setDefaultPresendScript(descriptor.getDefaultPresendScript());
            try {
                descriptor.setDefaultClasspath(descriptor.getDefaultClasspath());
            } catch (FormException e) {
                //Some of the old configured classpaths probably used some environment variable, let's clean those out
                List<GroovyScriptPath> newList = new ArrayList<>();
                for (GroovyScriptPath path : descriptor.getDefaultClasspath()) {
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
                    descriptor.setDefaultClasspath(newList);
                } catch (FormException e1) {
                    assert false : e1;
                }
            }
        }
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return Messages.ExtendedEmailPublisherDescriptor_DisplayName();
    }

    public String getAdminAddress() {
        JenkinsLocationConfiguration config = JenkinsLocationConfiguration.get();
        if(config != null) {
            if(StringUtils.isBlank(mailAccount.getAddress())) {
                return config.getAdminAddress();
            }
        }
        return mailAccount.getAddress();
    }

    public String getDefaultSuffix() {
        return defaultSuffix;
    }

    @DataBoundSetter
    public void setDefaultSuffix(String defaultSuffix) {
        this.defaultSuffix = Util.fixEmptyAndTrim(defaultSuffix);
    }

    @Restricted(NoExternalUse.class)
    Session createSession(MailAccount acc, ExtendedEmailPublisherContext context) {
        final String SMTP_PORT_PROPERTY = "mail.smtp.port";
        final String SMTP_SOCKETFACTORY_PORT_PROPERTY = "mail.smtp.socketFactory.port";

        Properties props = new Properties(System.getProperties());

        if (acc.getSmtpHost() != null) {
            props.put("mail.smtp.host", acc.getSmtpHost());
        }
        if (acc.getSmtpPort() != null) {
            props.put(SMTP_PORT_PROPERTY, acc.getSmtpPort());
        }
        if (acc.isUseSsl()) {
            /* This allows the user to override settings by setting system properties but
             * also allows us to use the default SMTPs port of 465 if no port is already set.
             * It would be cleaner to use smtps, but that's done by calling session.getTransport()...
             * and thats done in mail sender, and it would be a bit of a hack to get it all to
             * coordinate, and we can make it work through setting mail.smtp properties.
             */
            if (props.getProperty(SMTP_SOCKETFACTORY_PORT_PROPERTY) == null) {
                String port = acc.getSmtpPort() == null ? "465" : mailAccount.getSmtpPort();
                props.put(SMTP_PORT_PROPERTY, port);
                props.put(SMTP_SOCKETFACTORY_PORT_PROPERTY, port);
            }
            if (props.getProperty("mail.smtp.socketFactory.class") == null) {
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }
            props.put("mail.smtp.socketFactory.fallback", "false");

            // RFC 2595 specifies additional checks that must be performed on the server's
            // certificate to ensure that the server you connected to is the server you intended
            // to connect to. This reduces the risk of "man in the middle" attacks.
            if (props.getProperty("mail.smtp.ssl.checkserveridentity") == null) {
                props.put("mail.smtp.ssl.checkserveridentity", "true");
            }
        }
        if (acc.isUseTls()) {
            /* This allows the user to override settings by setting system properties and
             * also allows us to use the default STARTTLS port, 587, if no port is already set.
             * Only the properties included below are required to use STARTTLS and they are
             * not expected to be enabled simultaneously with SSL (it will actually throw a
             * "javax.net.ssl.SSLException: Unrecognized SSL message, plaintext connection?"
             * if SMTP server expects only TLS).
             */
            if (props.getProperty(SMTP_SOCKETFACTORY_PORT_PROPERTY) == null) {
                String port = acc.getSmtpPort() == null ? "587" : mailAccount.getSmtpPort();
                props.put(SMTP_PORT_PROPERTY, port);
                props.put(SMTP_SOCKETFACTORY_PORT_PROPERTY, port);
            }
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        if (!StringUtils.isBlank(acc.getCredentialsId())) {
            props.put("mail.smtp.auth", "true");
        }

        // avoid hang by setting some timeout.
        props.put("mail.smtp.timeout", "60000");
        props.put("mail.smtp.connectiontimeout", "60000");

        try {
            String ap = acc.getAdvProperties();
            if (ap != null && !StringUtils.isBlank(ap.trim())) {
                props.load(new StringReader(ap));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Parameters parse fail.", e);
        }

        return Session.getInstance(props, getAuthenticator(acc, context));
    }

    private Authenticator getAuthenticator(final MailAccount acc, final ExtendedEmailPublisherContext context) {
        if (acc == null || StringUtils.isBlank(acc.getCredentialsId())) {
            return null;
        }
        return authenticatorProvider.apply(acc, context.getRun());
    }

    public String getHudsonUrl() {
        return Jenkins.get().getRootUrl();
    }

    public List<MailAccount> getAddAccounts() {
        return addAccounts;
    }

    @DataBoundSetter
    public void setAddAccounts(List<MailAccount> addAccounts) {
        this.addAccounts = addAccounts;
    }

    @Deprecated
    public String getSmtpServer() {
        return mailAccount.getSmtpHost();
    }

    @Deprecated
    public void setSmtpServer(String smtpServer) {
        mailAccount.setSmtpHost(smtpServer);
    }

    @Deprecated
    public String getSmtpUsername() {
        return mailAccount.getSmtpUsername();
    }

    @SuppressWarnings("unused")
    @Deprecated
    public void setSmtpUsername(String username) {
        mailAccount.setSmtpUsername(username);
    }

    @Deprecated
    public Secret getSmtpPassword() {
        return mailAccount.getSmtpPassword();
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    @Deprecated
    public void setSmtpPassword(String password) {
        mailAccount.setSmtpPassword(password);
    }

    // Make API match Mailer plugin
    @SuppressWarnings("unused")
    public void setSmtpAuth(String userName, String password) {
        mailAccount.setSmtpUsername(userName);
        mailAccount.setSmtpPassword(password);
    }

    @Deprecated
    public boolean getUseSsl() {
        return mailAccount.isUseSsl();
    }

    @SuppressWarnings("unused")
    @Deprecated
    public void setUseSsl(boolean useSsl) {
        mailAccount.setUseSsl(useSsl);
    }

    @Deprecated
    public String getSmtpPort() {
        return mailAccount.getSmtpPort();
    }

    @SuppressWarnings("unused")
    @Deprecated
    public void setSmtpPort(String port) {
        mailAccount.setSmtpPort(nullify(port));
    }

    @Deprecated
    public String getAdvProperties() {
        return mailAccount.getAdvProperties();
    }

    @Deprecated
    public void setAdvProperties(String advProperties) {
        mailAccount.setAdvProperties(advProperties);
    }

    public String getCharset() {
        String c = charset;
        if (StringUtils.isBlank(c)) {
            c = "UTF-8";
        }
        return c;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setCharset(String charset) {
        this.charset = Util.fixEmptyAndTrim(charset);
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setDefaultContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            this.defaultContentType = "text/plain";
        } else {
            this.defaultContentType = contentType;
        }
    }

    public FormValidation doCheckDefaultSuffix(@QueryParameter String value) {
        if (value.matches("@[A-Za-z0-9.\\-]+") || Util.fixEmptyAndTrim(value)==null)
            return FormValidation.ok();
        else
            return FormValidation.error(Messages.Mailer_Suffix_Error());
    }

    public String getDefaultSubject() {
        return defaultSubject;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
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
    @DataBoundSetter
    public void setDefaultBody(String body) {
        if (StringUtils.isBlank(body)) {
            this.defaultBody = ExtendedEmailPublisher.DEFAULT_BODY_TEXT;
        } else {
            this.defaultBody = body;
        }
    }

    public String getEmergencyReroute() {
        return emergencyReroute;
    }

    @DataBoundSetter
    public void setEmergencyReroute(String emergencyReroute) {
        if (StringUtils.isBlank(emergencyReroute)) {
            this.emergencyReroute = ExtendedEmailPublisher.DEFAULT_EMERGENCY_REROUTE_TEXT;
        } else {
            this.emergencyReroute = Util.fixEmptyAndTrim(emergencyReroute);
        }
    }

    public long getMaxAttachmentSize() {
        return maxAttachmentSize;
    }

    @DataBoundSetter
    public void setMaxAttachmentSize(long bytes) {
        if (bytes < 0) {
            bytes = -1; // set to default "empty" value
        }
        this.maxAttachmentSize = bytes;
    }

    public MailAccount getMailAccount() {
        return mailAccount;
    }

    @DataBoundSetter
    public void setMailAccount(MailAccount mailAccount) {
        this.mailAccount = mailAccount;
        this.mailAccount.setDefaultAccount(true);
    }

    public long getMaxAttachmentSizeMb() {
        if(maxAttachmentSize < 0) {
            return -1;
        }
        return maxAttachmentSize / (1024 * 1024);
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setMaxAttachmentSizeMb(long mb) {
        if(mb < 0) {
            setMaxAttachmentSize(mb);
        } else {
            setMaxAttachmentSize(mb * (1024 * 1024));
        }
    }

    public String getDefaultRecipients() {
        return recipientList;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setDefaultRecipients(String recipients) {
        this.recipientList = ((recipients == null) ? "" : recipients);
    }

    public String getAllowedDomains() {
        return allowedDomains;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setAllowedDomains(String allowed) {
        this.allowedDomains = ((allowed == null) ? "" : allowed);
    }

    public String getExcludedCommitters() {
        return excludedCommitters;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setExcludedCommitters(String excluded) {
        this.excludedCommitters = ((excluded == null) ? "" : excluded);
    }

    @Deprecated
    public boolean getOverrideGlobalSettings() {
        return overrideGlobalSettings;
    }

    public String getListId() {
        return listId;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setListId(String id) {
        this.listId = id;
    }

    public boolean getPrecedenceBulk() {
        return precedenceBulk;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setPrecedenceBulk(boolean bulk) {
        this.precedenceBulk = bulk;
    }

    public String getDefaultReplyTo() {
        return defaultReplyTo;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
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
    @DataBoundSetter
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
    @DataBoundSetter
    public void setWatchingEnabled(boolean enabled) {
        this.enableWatching = enabled;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setAllowUnregisteredEnabled(boolean enabled) {
        this.enableAllowUnregistered = enabled;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    public String getDefaultPresendScript() {
        return defaultPresendScript;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
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
    @DataBoundSetter
    public void setDefaultPostsendScript(String script) {
        script = StringUtils.trim(script);
        this.defaultPostsendScript = ScriptApproval.get().configuring(((script == null) ? "" : script),
                GroovyLanguage.get(),
                ApprovalContext.create().withCurrentUser());
    }

    public List<GroovyScriptPath> getDefaultClasspath() {
        return defaultClasspath;
    }

    @DataBoundSetter
    public void setDefaultClasspath(List<GroovyScriptPath> defaultClasspath) throws FormException {
        if (Jenkins.get().isUseSecurity()) {
            ScriptApproval approval = ScriptApproval.get();
            ApprovalContext context = ApprovalContext.create().withCurrentUser();
            for (GroovyScriptPath path : defaultClasspath) {
                URL u = path.asURL();
                if (u != null) {
                    try {
                        approval.configuring(new ClasspathEntry(u.toString()), context);
                    } catch (MalformedURLException e) {
                        throw new FormException(e, "defaultClasspath");
                    }
                }
            }
        }
        this.defaultClasspath = defaultClasspath;
    }

    public List<String> getDefaultTriggerIds() {
        if (defaultTriggerIds.isEmpty()) {
            if (!defaultTriggers.isEmpty()) {
                for (EmailTriggerDescriptor t : this.defaultTriggers) {
                    // we have to do the below because a bunch of stuff is not serialized for the Descriptor
                    EmailTriggerDescriptor d = Jenkins.get().getDescriptorByType(t.getClass());
                    if (d != null && !defaultTriggerIds.contains(d.getId())) {
                        defaultTriggerIds.add(d.getId());
                    }
                }
            } else {
                FailureTrigger.DescriptorImpl f = Jenkins.get().getDescriptorByType(FailureTrigger.DescriptorImpl.class);
                if (f != null) {
                    defaultTriggerIds.add(f.getId());
                }
            }
            save();
        }
        return defaultTriggerIds;
    }

    @DataBoundSetter
    public void setDefaultTriggerIds(List<String> triggerIds) {
        defaultTriggerIds = triggerIds;
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillDefaultContentTypeItems() {
        ListBoxModel items = new ListBoxModel();
        items.add(Messages.contentType_plainText(), "text/plain");
        items.add(Messages.contentType_html(), "text/html");
        return items;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws FormException {
        req.bindJSON(this, formData);
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

    public FormValidation doAddressCheck(@QueryParameter final String value) {
        try {
            new InternetAddress(value);
            return FormValidation.ok();
        } catch (AddressException e) {
            return FormValidation.error(e.getMessage());
        }
    }

    public FormValidation doRecipientListRecipientsCheck(@QueryParameter final String value) {
        return new EmailRecipientUtils().validateFormRecipientList(value);
    }

    public FormValidation doMaxAttachmentSizeCheck(@QueryParameter final String value) {
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

    @NonNull
    @Override
    public Permission getRequiredGlobalConfigPagePermission() {
        return Jenkins.MANAGE;
    }

    BiFunction<MailAccount, Run<?, ?>, Authenticator> getAuthenticatorProvider() {
        return authenticatorProvider;
    }

    void setAuthenticatorProvider(BiFunction<MailAccount, Run<?,?>, Authenticator> authenticatorProvider) {
        this.authenticatorProvider = authenticatorProvider;
    }
}
