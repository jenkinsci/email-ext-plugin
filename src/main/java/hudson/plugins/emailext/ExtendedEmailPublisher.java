package hudson.plugins.emailext;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Cause.UserCause;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.emailext.plugins.ContentBuilder;
import hudson.plugins.emailext.plugins.CssInliner;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.MailMessageIdAction;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Mailer;

import jenkins.model.Jenkins;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.FilePath;
import hudson.model.Action;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import org.kohsuke.groovy.sandbox.SandboxTransformer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * {@link Publisher} that sends notification e-mail.
 */
public class ExtendedEmailPublisher extends Notifier implements MatrixAggregatable {

    private static final Logger LOGGER = Logger.getLogger(ExtendedEmailPublisher.class.getName());

    private static final String CONTENT_TRANSFER_ENCODING = System.getProperty(ExtendedEmailPublisher.class.getName() + ".Content-Transfer-Encoding");

    public static final Map<String, EmailTriggerDescriptor> EMAIL_TRIGGER_TYPE_MAP = new HashMap<String, EmailTriggerDescriptor>();
    
    public static final String DEFAULT_RECIPIENTS_TEXT = "";

    public static final String DEFAULT_SUBJECT_TEXT = "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!";

    public static final String DEFAULT_BODY_TEXT = "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:\n\n"
            + "Check console output at $BUILD_URL to view the results.";

    public static final String DEFAULT_EMERGENCY_REROUTE_TEXT = "";
    
    public static final String DEFAULT_SYSTEM_ADMINISTRATOR_TEXT = "";
	
    public static final String PROJECT_DEFAULT_SUBJECT_TEXT = "$PROJECT_DEFAULT_SUBJECT";

    public static final String PROJECT_DEFAULT_BODY_TEXT = "$PROJECT_DEFAULT_CONTENT";
    
    public static void addEmailTriggerType(EmailTriggerDescriptor triggerType) throws EmailExtException {
        if (EMAIL_TRIGGER_TYPE_MAP.containsKey(triggerType.getMailerId())) {
            throw new EmailExtException("An email trigger type with name "
                    + triggerType.getTriggerName() + " was already added.");
        }
        EMAIL_TRIGGER_TYPE_MAP.put(triggerType.getMailerId(), triggerType);
    }

    public static void removeEmailTriggerType(EmailTriggerDescriptor triggerType) {
        if (EMAIL_TRIGGER_TYPE_MAP.containsKey(triggerType.getMailerId())) {
            EMAIL_TRIGGER_TYPE_MAP.remove(triggerType.getMailerId());
        }
    }

    public static EmailTriggerDescriptor getEmailTriggerType(String mailerId) {
        return EMAIL_TRIGGER_TYPE_MAP.get(mailerId);
    }

    public static Collection<EmailTriggerDescriptor> getEmailTriggers() {
        return EMAIL_TRIGGER_TYPE_MAP.values();
    }

    public static Collection<String> getEmailTriggerNames() {
        return EMAIL_TRIGGER_TYPE_MAP.keySet();
    }

    public static List<EmailTrigger> getTriggersForNonConfiguredInstance() {
        List<EmailTrigger> retList = new ArrayList<EmailTrigger>();
        for (String mailerId : EMAIL_TRIGGER_TYPE_MAP.keySet()) {
            retList.add(EMAIL_TRIGGER_TYPE_MAP.get(mailerId).getNewInstance(null, null, null));
        }
        return retList;
    }

    /**
     * A comma-separated list of email recipient that will be used for every trigger.
     */
    public String recipientList = "";

    /** This is the list of email triggers that the project has configured */
    public List<EmailTrigger> configuredTriggers = new ArrayList<EmailTrigger>();

    /**
     * The contentType of the emails for this project (text/html, text/plain, etc).
     */
    public String contentType;

    /**
     * The default subject of the emails for this project.  ($PROJECT_DEFAULT_SUBJECT)
     */
    public String defaultSubject;

    /**
     * The default body of the emails for this project.  ($PROJECT_DEFAULT_BODY)
     */
    public String defaultContent;
    
    /**
     * The project wide set of attachments.
     */
    public String attachmentsPattern;
    
    /**
     * The project's pre-send script.
     */
    public String presendScript;

    /**
     * True to attach the log from the build to the email.
     */
    public boolean attachBuildLog;

    /**
     * True to compress the log from the build before attaching to the email
     */
    public boolean compressBuildLog;

    /**
     * Reply-To value for the e-mail
     */
    public String replyTo;
    
    /**
     * If true, save the generated email content to email-ext-message.[txt|html]
     */
    public boolean saveOutput = false;

    private MatrixTriggerMode matrixTriggerMode;

    /**
     * Get the list of configured email triggers for this project.
     */
    public List<EmailTrigger> getConfiguredTriggers() {
        if (configuredTriggers == null) {
            configuredTriggers = new ArrayList<EmailTrigger>();
        }
        return configuredTriggers;
    }

    /**
     * Get the list of non-configured email triggers for this project.
     */
    public List<EmailTrigger> getNonConfiguredTriggers() {
        List<EmailTrigger> confTriggers = getConfiguredTriggers();

        List<EmailTrigger> retList = new ArrayList<EmailTrigger>();
        for (String mailerId : EMAIL_TRIGGER_TYPE_MAP.keySet()) {
            boolean contains = false;
            for (EmailTrigger trigger : confTriggers) {
                if (trigger.getDescriptor().getMailerId().equals(mailerId)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                retList.add(EMAIL_TRIGGER_TYPE_MAP.get(mailerId).getNewInstance(null, null, null));
            }
        }
        return retList;
    }

    /**
     * Return true if the project has been configured, otherwise returns false
     */
    public boolean isConfigured() {
        return !getConfiguredTriggers().isEmpty();
    }

    /**
     * Return true if the project has been configured, otherwise returns false
     */
    public boolean getConfigured() {
        return isConfigured();
    }

    public MatrixTriggerMode getMatrixTriggerMode() {
        if (matrixTriggerMode ==null)    return MatrixTriggerMode.BOTH;
        return matrixTriggerMode;
    }

    public void setMatrixTriggerMode(MatrixTriggerMode matrixTriggerMode) {
        this.matrixTriggerMode = matrixTriggerMode;
    }

    public void debug(PrintStream p, String format, Object... args) {
        ExtendedEmailPublisher.DESCRIPTOR.debug(p, format, args);
    }
    
    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?,?> project) {
        return Collections.singletonList(new EmailExtTemplateAction(project));
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        debug(listener.getLogger(), "Checking for pre-build");
        if (!(build instanceof MatrixRun) || isExecuteOnMatrixNodes()) {
            debug(listener.getLogger(), "Executing pre-build step");
            return _perform(build, listener, true);
        }
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        debug(listener.getLogger(), "Checking for post-build");
        if (!(build instanceof MatrixRun) || isExecuteOnMatrixNodes()) {
            debug(listener.getLogger(), "Performing post-build step");
            return _perform(build, listener, false);
        }
        return true;
    }

    private boolean _perform(AbstractBuild<?, ?> build, BuildListener listener, boolean forPreBuild) {
        boolean emailTriggered = false;
        debug(listener.getLogger(), "Checking if email needs to be generated");
        Map<String, EmailTrigger> triggered = new HashMap<String, EmailTrigger>();

        for (EmailTrigger trigger : configuredTriggers) {
            if (trigger.isPreBuild() == forPreBuild && trigger.trigger(build, listener)) {
                String tName = trigger.getDescriptor().getTriggerName();
                triggered.put(tName, trigger);
                listener.getLogger().println("Email was triggered for: " + tName);
                emailTriggered = true;
            }
        }

        //Go through and remove triggers that are replaced by others
        List<String> replacedTriggers = new ArrayList<String>();

        for (String triggerName : triggered.keySet()) {
            replacedTriggers.addAll(triggered.get(triggerName).getDescriptor().getTriggerReplaceList());
        }
        for (String triggerName : replacedTriggers) {
            triggered.remove(triggerName);
            listener.getLogger().println("Trigger " + triggerName + " was overridden by another trigger and will not send an email.");
        }

        if (emailTriggered && triggered.isEmpty()) {
            listener.getLogger().println("There is a circular trigger replacement with the email triggers.  No email is sent.");
            return false;
        } else if (triggered.isEmpty()) {
            listener.getLogger().println("No emails were triggered.");
            return true;
        }

        for (String triggerName : triggered.keySet()) {
            listener.getLogger().println("Sending email for trigger: " + triggerName);
            sendMail(triggered.get(triggerName).getEmail(), build, listener, triggered.get(triggerName), triggered);
        }

        return true;
    }

    private boolean sendMail(EmailType mailType, AbstractBuild<?, ?> build, BuildListener listener, EmailTrigger trigger, Map<String, EmailTrigger> triggered) {
        try {
            MimeMessage msg = createMail(mailType, build, listener, trigger);
            debug(listener.getLogger(), "Successfully created MimeMessage");
            Address[] allRecipients = msg.getAllRecipients();
            int retries = 0;
            if (allRecipients != null) {
                StringBuilder buf = new StringBuilder("Sending email to:");
                for (Address a : allRecipients) {
                    buf.append(' ').append(a);
                }
                listener.getLogger().println(buf);
                if(executePresendScript(build, listener, msg, trigger, triggered)) {
                    while(true) {
                        try {
                            Transport.send(msg);
                            break;
                        } catch (SendFailedException e) {
                            if(e.getNextException() != null && 
                                    ((e.getNextException() instanceof SocketException) || 
                                    (e.getNextException() instanceof ConnectException))) {
                                listener.getLogger().println("Socket error sending email, retrying once more in 10 seconds...");
                                Thread.sleep(10000);
                            } else {
                                Address[] addresses = e.getValidSentAddresses();
                                if(addresses != null && addresses.length > 0) {
                                    buf = new StringBuilder("Successfully sent to the following addresses:");
                                    for (Address a : addresses) {
                                        buf.append(' ').append(a);
                                    }
                                    listener.getLogger().println(buf);
                                }
                                addresses = e.getValidUnsentAddresses();
                                if(addresses != null && addresses.length > 0) {
                                    buf = new StringBuilder("Error sending to the following VALID addresses:");
                                    for (Address a : addresses) {
                                        buf.append(' ').append(a);
                                    }
                                    listener.getLogger().println(buf);
                                }
                                addresses = e.getInvalidAddresses();
                                if(addresses != null && addresses.length > 0) {
                                    buf = new StringBuilder("Error sending to the following INVALID addresses:");
                                    for (Address a : addresses) {
                                        buf.append(' ').append(a);
                                    }
                                    listener.getLogger().println(buf);
                                }

                                debug(listener.getLogger(), "SendFailedException message: " + e.getMessage());
                                break;
                            }
                        } 
                        retries++;
                        if(retries > 1) {
                            listener.getLogger().println("Failed after second try sending email");
                            break;
                        }
                    }
                    if (build.getAction(MailMessageIdAction.class) == null) {
                        build.addAction(new MailMessageIdAction(msg.getMessageID()));
                    }
                } else {
                    listener.getLogger().println("Email sending was cancelled" 
                        + " by user script.");                        
                }
                return true;
            } else {
                listener.getLogger().println("An attempt to send an e-mail"
                        + " to empty list of recipients, ignored.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not send email.", e);
            e.printStackTrace(listener.error("Could not send email as a part of the post-build publishers."));
        }

        debug(listener.getLogger(), "Some error occured trying to send the email...check the Jenkins log");
        return false;
    }

    private boolean executePresendScript(AbstractBuild<?, ?> build, BuildListener listener, MimeMessage msg, EmailTrigger trigger, Map<String, EmailTrigger> triggered)
            throws RuntimeException {
        boolean cancel = false;
        presendScript = new ContentBuilder().transformText(presendScript, this, build, listener);
        if (StringUtils.isNotBlank(presendScript)) {
            listener.getLogger().println("Executing pre-send script");
            ClassLoader cl = Jenkins.getInstance().getPluginManager().uberClassLoader;
            ScriptSandbox sandbox = null;
            CompilerConfiguration cc = new CompilerConfiguration();
            cc.addCompilationCustomizers(new ImportCustomizer().addStarImports(
                    "jenkins",
                    "jenkins.model",
                    "hudson",
                    "hudson.model"));

            if(ExtendedEmailPublisher.DESCRIPTOR.isSecurityEnabled()) {
                debug(listener.getLogger(), "Setting up sandbox for pre-send script");
                cc.addCompilationCustomizers(new SandboxTransformer());
                sandbox = new ScriptSandbox();
            }

            Binding binding = new Binding();
            binding.setVariable("build", build);
            binding.setVariable("msg", msg);
            binding.setVariable("logger", listener.getLogger());
            binding.setVariable("cancel", cancel);
            binding.setVariable("trigger", trigger);
            binding.setVariable("triggered", Collections.unmodifiableMap(triggered));

            GroovyShell shell = new GroovyShell(cl, binding, cc);
            StringWriter out = new StringWriter();
            PrintWriter pw = new PrintWriter(out);

            if(sandbox != null) {
                sandbox.register();
            }

            try {
                Object output = shell.evaluate(presendScript);
                if(output!=null) {
                    pw.println("Result: "+output);
                    cancel = ((Boolean)shell.getVariable("cancel")).booleanValue();
                    debug(listener.getLogger(), "Pre-send script set cancel to %b", cancel);
                }
            } catch (SecurityException e) {
                listener.getLogger().println("Pre-send script tried to access secured objects: " + e.getMessage());
            } catch (Throwable t) {
                t.printStackTrace(pw);
                listener.getLogger().println(out.toString());
                // should we cancel the sending of the email???
            }
            debug(listener.getLogger(), out.toString());
        }            
        return !cancel;
    }    

    private MimeMessage createMail(EmailType type, AbstractBuild<?, ?> build, BuildListener listener, EmailTrigger trigger) throws MessagingException, IOException, InterruptedException {
        boolean overrideGlobalSettings = ExtendedEmailPublisher.DESCRIPTOR.getOverrideGlobalSettings();

        MimeMessage msg;

        // If not overriding global settings, use the Mailer class to create a session and set the from address
        // Else we'll do it ourselves
        if (!overrideGlobalSettings) {
            debug(listener.getLogger(), "NOT overriding default server settings, using Mailer to create session");
            msg = new MimeMessage(Mailer.descriptor().createSession());
            msg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
        } else {
            debug(listener.getLogger(), "Overriding default server settings, creating our own session");
            msg = new MimeMessage(ExtendedEmailPublisher.DESCRIPTOR.createSession());
            msg.setFrom(new InternetAddress(ExtendedEmailPublisher.DESCRIPTOR.getAdminAddress()));
        }

        String charset = Mailer.descriptor().getCharset();
        if (overrideGlobalSettings) {
            String overrideCharset = ExtendedEmailPublisher.DESCRIPTOR.getCharset();
            if (StringUtils.isNotBlank(overrideCharset)) {
                debug(listener.getLogger(), "Overriding charset %s", overrideCharset);
                charset = overrideCharset;
            }
        }

        // Set the contents of the email
        msg.addHeader("X-Jenkins-Job", build.getProject().getDisplayName());
        if(build.getResult() != null) {
            msg.addHeader("X-Jenkins-Result", build.getResult().toString());
        }
        msg.setSentDate(new Date());
        setSubject(type, build, msg, listener, charset);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(getContent(type, build, listener, charset, trigger));

        AttachmentUtils attachments = new AttachmentUtils(attachmentsPattern);
        attachments.attach(multipart, this, build, listener);

        // add attachments from the email type if they are setup
        if(StringUtils.isNotBlank(type.getAttachmentsPattern())) {
            AttachmentUtils typeAttachments = new AttachmentUtils(type.getAttachmentsPattern());
            typeAttachments.attach(multipart, this, build, listener);
        }

        if(attachBuildLog || type.getAttachBuildLog()) {
            debug(listener.getLogger(), "Request made to attach build log");
            AttachmentUtils.attachBuildLog(multipart, build, listener, compressBuildLog || type.getCompressBuildLog());
        }

        msg.setContent(multipart);
        
        EnvVars env = null;
        try {
            env = build.getEnvironment(listener);
        } catch(Exception e) {
            listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
            // create an empty set of env vars
            env = new EnvVars(); 
        }

        // Get the recipients from the global list of addresses
        Set<InternetAddress> recipientAddresses = new LinkedHashSet<InternetAddress>();
        Set<InternetAddress> ccAddresses = new LinkedHashSet<InternetAddress>();
        if (type.getSendToRecipientList()) {
            debug(listener.getLogger(), "Adding recipients from recipient list");
            addAddressesFromRecipientList(recipientAddresses, ccAddresses, getRecipientList(type, build, recipientList, listener, charset), env, listener);
        }
        // Get the list of developers who made changes between this build and the last
        // if this mail type is configured that way
        if (type.getSendToDevelopers()) {
            debug(listener.getLogger(), "Adding developers");
            Set<User> users;
            if (type.getIncludeCulprits()) {
                users = build.getCulprits();
            } else {
                users = new HashSet<User>();
                for (Entry change : build.getChangeSet()) {
                    users.add(change.getAuthor());
                }
            }
            
            for (User user : users) {
                if (!isExcludedCommitter(user.getFullName())) {
                    String userAddress = EmailRecipientUtils.getUserConfiguredEmail(user);
                    if (userAddress != null) {
                        addAddressesFromRecipientList(recipientAddresses, ccAddresses, userAddress, env, listener);
                    } else {
                        listener.getLogger().println("Failed to send e-mail to " + user.getFullName() + " because no e-mail address is known, and no default e-mail domain is configured");
                    }
                }
            }
        }

        if (type.isSendToRequester()) {
            debug(listener.getLogger(), "Sending to requester");
            // looking for Upstream build.
            AbstractBuild<?, ?> cur = build;
            Cause.UpstreamCause upc = build.getCause(Cause.UpstreamCause.class);
            while (upc != null) {
                // UpstreamCause.getUpStreamProject() returns the full name, so use getItemByFullName
                AbstractProject<?, ?> p = (AbstractProject<?, ?>) Hudson.getInstance().getItemByFullName(upc.getUpstreamProject());
                cur = p.getBuildByNumber(upc.getUpstreamBuild());
                upc = cur.getCause(Cause.UpstreamCause.class);
            }
            addUserTriggeringTheBuild(cur, recipientAddresses, ccAddresses, env, listener);
        }

        //Get the list of recipients that are uniquely specified for this type of email
        if (StringUtils.isNotBlank(type.getRecipientList())) {
            addAddressesFromRecipientList(recipientAddresses, ccAddresses, getRecipientList(type, build, type.getRecipientList(), listener, charset), env, listener);
        }

        String emergencyReroute = ExtendedEmailPublisher.DESCRIPTOR.getEmergencyReroute();
        boolean isEmergencyReroute = StringUtils.isNotBlank(emergencyReroute);
        
        if (isEmergencyReroute) {
          debug(listener.getLogger(), "Emergency reroute turned on");
          recipientAddresses.clear();
          addAddressesFromRecipientList(recipientAddresses, ccAddresses, emergencyReroute, env, listener);
          listener.getLogger().println("Emergency reroute is set to: " + emergencyReroute);
        }
        
        //Adding sys admin below emergency reroute so that it ALWAYS gets email.
        String sysAdmin = ExtendedEmailPublisher.DESCRIPTOR.getSystemAdministrator();
        boolean isSysAdmin = StringUtils.isNotBlank(sysAdmin);
        
        //Not using getAdminAddress so that we can configure emails, and send-to-all email.
        //If we want less flexibility, use this.
        //String sysAdmin = ExtendedEmailPublisher.DESCRIPTOR.getAdminAddress();
        if (isSysAdmin) {
          debug(listener.getLogger(), "Sending to System Administrator");
          addAddressesFromRecipientList(recipientAddresses, ccAddresses, sysAdmin, env, listener);
          debug(listener.getLogger(), "System Admin email is set to: " + sysAdmin);
        }
        
        msg.setRecipients(Message.RecipientType.TO, recipientAddresses.toArray(new InternetAddress[recipientAddresses.size()]));
        if(ccAddresses.size() > 0) {
            msg.setRecipients(Message.RecipientType.CC, ccAddresses.toArray(new InternetAddress[ccAddresses.size()]));
        }

        Set<InternetAddress> replyToAddresses = new LinkedHashSet<InternetAddress>();
        if (StringUtils.isNotBlank(replyTo)) {
            addAddressesFromRecipientList(replyToAddresses, null, getRecipientList(type, build, replyTo, listener, charset), env, listener);
        }

        if (StringUtils.isNotBlank(type.getReplyTo())) {
            addAddressesFromRecipientList(replyToAddresses, null, getRecipientList(type, build, type.getReplyTo(), listener, charset), env, listener);
        }

        if(replyToAddresses.size() > 0) {
            msg.setReplyTo(replyToAddresses.toArray(new InternetAddress[replyToAddresses.size()]));
        }

        AbstractBuild<?, ?> pb = build.getPreviousBuild();
        if (pb != null) {
            // Send mails as replies until next successful build
            MailMessageIdAction b = pb.getAction(MailMessageIdAction.class);
            if (b != null && pb.getResult() != Result.SUCCESS) {
                debug(listener.getLogger(), "Setting In-Reply-To since last build was not successful");
                msg.setHeader("In-Reply-To", b.messageId);
                msg.setHeader("References", b.messageId);
            }
        }

        if (CONTENT_TRANSFER_ENCODING != null) {
            msg.setHeader("Content-Transfer-Encoding", CONTENT_TRANSFER_ENCODING);
        }
        
        String listId = ExtendedEmailPublisher.DESCRIPTOR.getListId();
        if (listId != null) {
            msg.setHeader("List-ID", listId);
        }

        if (ExtendedEmailPublisher.DESCRIPTOR.getPrecedenceBulk()) {
            msg.setHeader("Precedence", "bulk");
        }

        return msg;
    }

    private boolean isExcludedCommitter(String userName) {
        StringTokenizer tokens = new StringTokenizer(DESCRIPTOR.getExcludedCommitters(), ",");
        while (tokens.hasMoreTokens()) {
            if (tokens.nextToken().trim().equalsIgnoreCase(userName)) {
                return true;
            }
        }
        return false;
    }

    private void addUserTriggeringTheBuild(AbstractBuild<?, ?> build, Set<InternetAddress> recipientAddresses, Set<InternetAddress> ccAddresses,
            EnvVars env, BuildListener listener) {
        User user = getByUserIdCause(build);
        if (user == null) {
           user = getByLegacyUserCause(build);
        }
                
        if (user != null) {
            String adrs = user.getProperty(Mailer.UserProperty.class).getAddress();
            if (adrs != null) {
                addAddressesFromRecipientList(recipientAddresses, ccAddresses, adrs, env, listener);
            } else {
                listener.getLogger().println("Failed to send e-mail to " + user.getFullName() + " because no e-mail address is known, and no default e-mail domain is configured");
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private User getByUserIdCause(AbstractBuild<?, ?> build) {
        try {
            Class<? extends Cause> userIdCause = (Class<? extends Cause>)
                    ExtendedEmailPublisher.class.getClassLoader().loadClass("hudson.model.Cause$UserIdCause");
            Method getUserId = userIdCause.getMethod("getUserId", new Class[0]);
            
            Cause cause = build.getCause(userIdCause);
            if (cause != null) {
                String id = (String) getUserId.invoke(cause, new Object[0]);
                return User.get(id, false);
            }
            
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        return null;
    }
    
    private User getByLegacyUserCause(AbstractBuild<?, ?> build) {
        try {
            UserCause userCause = build.getCause(Cause.UserCause.class);
            // userCause.getUserName() returns displayName which may be different from authentication name
            // Therefore use reflection to access the real authenticationName
            if (userCause != null) {
                Field authenticationName = UserCause.class.getDeclaredField("authenticationName");
                authenticationName.setAccessible(true);
                String name = (String) authenticationName.get(userCause);
                return User.get(name, false);
            }
        } catch(Exception e) {
            LOGGER.info(e.getMessage());
        }
        return null;
    }

    private void setSubject(final EmailType type, final AbstractBuild<?, ?> build, MimeMessage msg, BuildListener listener, String charset)
            throws MessagingException {
        String subject = new ContentBuilder().transformText(type.getSubject(), this, build, listener);
        msg.setSubject(subject, charset);
    }
    
    private String getRecipientList(final EmailType type, final AbstractBuild<?, ?> build, String recipients, BuildListener listener, String charset)
        throws MessagingException {
        final String recipientsTransformed = StringUtils.isBlank(recipients) ? "" : new ContentBuilder().transformText(recipients, this, build, listener);
        return recipientsTransformed;
    }

    public boolean isExecuteOnMatrixNodes() {
        MatrixTriggerMode mtm = getMatrixTriggerMode();
        return MatrixTriggerMode.BOTH == mtm
            || MatrixTriggerMode.ONLY_CONFIGURATIONS == mtm;
    }

    private MimeBodyPart getContent(final EmailType type, final AbstractBuild<?, ?> build, BuildListener listener, String charset, EmailTrigger trigger)
            throws MessagingException {
        final String text = new ContentBuilder().transformText(type.getBody(), this, build, listener);
        
        String messageContentType = contentType;
        // contentType is null if the project was not reconfigured after upgrading.
        if (messageContentType == null || "default".equals(messageContentType)) {
            messageContentType = DESCRIPTOR.getDefaultContentType();
            // The defaultContentType is null if the main Jenkins configuration
            // was not reconfigured after upgrading.
            if (messageContentType == null) {
                messageContentType = "text/plain";
            }
        }
        messageContentType += "; charset=" + charset;
        
        try {
            if(saveOutput) {
                Random random = new Random();
                String extension = ".html";
                if(messageContentType.startsWith("text/plain")) {
                    extension = ".txt";
                }

                FilePath savedOutput = new FilePath(build.getWorkspace(), 
                        String.format("%s-%s%d%s", trigger.getDescriptor().getTriggerName(), build.getId(), random.nextInt(), extension));
                savedOutput.write(text, charset);
            }
        } catch(IOException e) {
            listener.getLogger().println("Error trying to save email output to file. " + e.getMessage());
        } catch(InterruptedException e) {
            listener.getLogger().println("Error trying to save email output to file. " + e.getMessage());
        }

        // set the email message text 
        // (plain text or HTML depending on the content type)
        MimeBodyPart msgPart = new MimeBodyPart();
        debug(listener.getLogger(), "messageContentType = %s", messageContentType);
        if (messageContentType.startsWith("text/html")) {
            String inlinedCssHtml = new CssInliner().process(text);
            msgPart.setContent(inlinedCssHtml, messageContentType);
        } else {
            msgPart.setContent(text, messageContentType);
        }
        return msgPart;
    }   

    private static void addAddressesFromRecipientList(Set<InternetAddress> addresses, Set<InternetAddress> ccAddresses, String recipientList,
            EnvVars envVars, BuildListener listener) {
        try {
            Set<InternetAddress> internetAddresses = new EmailRecipientUtils().convertRecipientString(recipientList, envVars, EmailRecipientUtils.TO);
            addresses.addAll(internetAddresses);
            if(ccAddresses != null) {
                Set<InternetAddress> ccInternetAddresses = new EmailRecipientUtils().convertRecipientString(recipientList, envVars, EmailRecipientUtils.CC);
                ccAddresses.addAll(ccInternetAddresses);
            }
        } catch (AddressException ae) {
            LOGGER.log(Level.WARNING, "Could not create email address.", ae);
            listener.getLogger().println("Failed to create e-mail address for " + ae.getRef());
        } catch(UnsupportedEncodingException e) {
            LOGGER.log(Level.WARNING, "Could not create email address.", e);
            listener.getLogger().println("Failed to create e-mail address because of invalid encoding");
        }
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final ExtendedEmailPublisherDescriptor DESCRIPTOR = new ExtendedEmailPublisherDescriptor();

    // The descriptor has been moved but we need to maintain the old descriptor for backwards compatibility reasons.
    @SuppressWarnings({"UnusedDeclaration"})
    public static final class DescriptorImpl
            extends ExtendedEmailPublisherDescriptor {
    }

    public MatrixAggregator createAggregator(MatrixBuild matrixbuild,
            Launcher launcher, BuildListener buildlistener) {
        return new MatrixAggregator(matrixbuild, launcher, buildlistener) {
            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                LOGGER.log(Level.FINER, "end build of " + this.build.getDisplayName());

                // Will be run by parent so we check if needed to be executed by parent
                if (getMatrixTriggerMode().forParent) {
                    return ExtendedEmailPublisher.this._perform(this.build, this.listener, false);
                }
                return true;
            }

            @Override
            public boolean startBuild() throws InterruptedException, IOException {
                LOGGER.log(Level.FINER, "end build of " + this.build.getDisplayName());
                // Will be run by parent so we check if needed to be executed by parent 
                if (getMatrixTriggerMode().forParent) {
                    return ExtendedEmailPublisher.this._perform(this.build, this.listener, true);
                }
                return true;
            }
        };
    }
}
