package hudson.plugins.emailext;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.emailext.plugins.ContentBuilder;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.MailMessageIdAction;
import hudson.tasks.Mailer;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Publisher} that sends notification e-mail.
 */
public class ExtendedEmailPublisher extends Notifier {

	private static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

	public static final Map<String,EmailTriggerDescriptor> EMAIL_TRIGGER_TYPE_MAP = new HashMap<String,EmailTriggerDescriptor>();

	public static final String DEFAULT_SUBJECT_TEXT = "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!";
	public static final String DEFAULT_BODY_TEXT = "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:\n\n" +
		"Check console output at $BUILD_URL to view the results.";

	public static final String PROJECT_DEFAULT_SUBJECT_TEXT = "$PROJECT_DEFAULT_SUBJECT";
	public static final String PROJECT_DEFAULT_BODY_TEXT = "$PROJECT_DEFAULT_CONTENT";

    public static final String CHARSET = "utf-8";

	public static void addEmailTriggerType(EmailTriggerDescriptor triggerType) throws EmailExtException {
		if(EMAIL_TRIGGER_TYPE_MAP.containsKey(triggerType.getMailerId()))
			throw new EmailExtException("An email trigger type with name " +
					triggerType.getTriggerName() + " was already added.");
		EMAIL_TRIGGER_TYPE_MAP.put(triggerType.getMailerId(), triggerType);
	}

	public static void removeEmailTriggerType(EmailTriggerDescriptor triggerType) {
		if(EMAIL_TRIGGER_TYPE_MAP.containsKey(triggerType.getMailerId()))
			EMAIL_TRIGGER_TYPE_MAP.remove(triggerType.getMailerId());
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
		for(String mailerId : EMAIL_TRIGGER_TYPE_MAP.keySet()) {
			retList.add(EMAIL_TRIGGER_TYPE_MAP.get(mailerId).getNewInstance(null));
		}
		return retList;
	}

	/**
	 * A comma-separated list of email recipient that will be used for every trigger.
	 */
	public String recipientList;

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
	 * Get the list of configured email triggers for this project.
	 */
	public List<EmailTrigger> getConfiguredTriggers() {
		if(configuredTriggers == null)
			configuredTriggers = new ArrayList<EmailTrigger>();
		return configuredTriggers;
	}

	/**
	 * Get the list of non-configured email triggers for this project.
	 */
	public List<EmailTrigger> getNonConfiguredTriggers() {
		List<EmailTrigger> confTriggers = getConfiguredTriggers();

		List<EmailTrigger> retList = new ArrayList<EmailTrigger>();
		for(String mailerId : EMAIL_TRIGGER_TYPE_MAP.keySet()) {
			boolean contains = false;
			for(EmailTrigger trigger : confTriggers) {
				if(trigger.getDescriptor().getMailerId().equals(mailerId)) {
					contains = true;
					break;
				}
			}
			if(!contains) {
				retList.add(EMAIL_TRIGGER_TYPE_MAP.get(mailerId).getNewInstance(null));
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

	@Override
	public boolean prebuild(AbstractBuild<?,?> build, BuildListener listener) {
		return _perform(build,listener,true);
	}

	@Override
	public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		return _perform(build,listener,false);
	}

	private boolean _perform(AbstractBuild<?,?> build, BuildListener listener, boolean forPreBuild) {
	   	boolean emailTriggered = false;

	   	Map<String,EmailTrigger> triggered = new HashMap<String, EmailTrigger>();

		for(EmailTrigger trigger : configuredTriggers) {
			if(trigger.isPreBuild() == forPreBuild && trigger.trigger(build)) {
				String tName = trigger.getDescriptor().getTriggerName();
				triggered.put(tName,trigger);
				listener.getLogger().println("Email was triggered for: " + tName);
				emailTriggered = true;
			}
		}

		//Go through and remove triggers that are replaced by others
		List<String> replacedTriggers = new ArrayList<String>();

		for(String triggerName : triggered.keySet()) {
			replacedTriggers.addAll(triggered.get(triggerName).getDescriptor().getTriggerReplaceList());
		}
		for(String triggerName : replacedTriggers) {
			triggered.remove(triggerName);
			listener.getLogger().println("Trigger " + triggerName + " was overridden by another trigger and will not send an email.");
		}

		if(emailTriggered && triggered.isEmpty()) {
			listener.getLogger().println("There is a circular trigger replacement with the email triggers.  No email is sent.");
			return false;
		}
		else if(triggered.isEmpty()) {
			listener.getLogger().println("No emails were triggered.");
			return true;
		}

		for(String triggerName :triggered.keySet()) {
			listener.getLogger().println("Sending email for trigger: " + triggerName);
			sendMail(triggered.get(triggerName).getEmail(), build, listener);
		}

		return true;
	}

	private boolean sendMail(EmailType mailType, AbstractBuild<?,?> build, BuildListener listener) {
		try {
			MimeMessage msg = createMail(mailType, build, listener);
			Address[] allRecipients = msg.getAllRecipients();
			if (allRecipients != null) {
				StringBuilder buf = new StringBuilder("Sending email to:");
				for (Address a : allRecipients)
					buf.append(' ').append(a);
				listener.getLogger().println(buf);
				Transport.send(msg);
				if (build.getAction(MailMessageIdAction.class) == null)
					build.addAction(new MailMessageIdAction(msg.getMessageID()));
				return true;
			} else {
				listener.getLogger().println("An attempt to send an e-mail"
					+ " to empty list of recipients, ignored.");
			}
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Could not send email.",e);
			e.printStackTrace(listener.error("Could not send email as a part of the post-build publishers."));
		}

		return false;
	}

	private MimeMessage createMail(EmailType type, AbstractBuild<?,?> build, BuildListener listener) throws MessagingException, IOException, InterruptedException {
		boolean overrideGlobalSettings = ExtendedEmailPublisher.DESCRIPTOR.getOverrideGlobalSettings();

		MimeMessage msg;

		// If not overriding global settings, use the Mailer class to create a session and set the from address
		// Else we'll do it ourselves
		if (!overrideGlobalSettings) {
			msg = new MimeMessage(Mailer.descriptor().createSession());
			msg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
		} else {
			msg = new MimeMessage(ExtendedEmailPublisher.DESCRIPTOR.createSession());
			msg.setFrom(new InternetAddress(ExtendedEmailPublisher.DESCRIPTOR.getAdminAddress()));
		}

        // Set the contents of the email

        msg.setSentDate(new Date());

        setSubject( type, build, msg );

        setContent( type, build, msg );

        EnvVars env = build.getEnvironment(listener);

		// Get the recipients from the global list of addresses
		List<InternetAddress> recipientAddresses = new ArrayList<InternetAddress>();
		if (type.getSendToRecipientList()) {
            addAddressesFromRecipientList(recipientAddresses, recipientList, env, listener);
		}
		// Get the list of developers who made changes between this build and the last
		// if this mail type is configured that way
		if (type.getSendToDevelopers()) {
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
				String adrs = user.getProperty(Mailer.UserProperty.class).getAddress();
				if (adrs != null)
					addAddressesFromRecipientList(recipientAddresses, adrs, env, listener);
				else {
					listener.getLogger().println("Failed to send e-mail to " + user.getFullName() + " because no e-mail address is known, and no default e-mail domain is configured");
				}
			}
		}
		//Get the list of recipients that are uniquely specified for this type of email
		if (type.getRecipientList() != null && type.getRecipientList().trim().length() > 0) {
            addAddressesFromRecipientList( recipientAddresses, type.getRecipientList(), env, listener );
		}

		msg.setRecipients(Message.RecipientType.TO, recipientAddresses.toArray(new InternetAddress[recipientAddresses.size()]));

		AbstractBuild<?,?> pb = build.getPreviousBuild();
		if (pb!=null) {
			// Send mails as replies until next successful build
			MailMessageIdAction b = pb.getAction(MailMessageIdAction.class);
			if(b!=null && pb.getResult()!=Result.SUCCESS) {
				msg.setHeader("In-Reply-To",b.messageId);
				msg.setHeader("References",b.messageId);
			}
		}

		return msg;
	}

    private void setSubject( final EmailType type, final AbstractBuild<?, ?> build, MimeMessage msg )
        throws MessagingException
    {
        String subject = new ContentBuilder().transformText(type.getSubject(), this, type, build);
        msg.setSubject(subject, CHARSET);
    }

    private void setContent( final EmailType type, final AbstractBuild<?, ?> build, MimeMessage msg )
        throws MessagingException
    {
        final String text = new ContentBuilder().transformText(type.getBody(), this, type, build);

        String messageContentType = contentType;
        // contentType is null if the project was not reconfigured after upgrading.
        if (messageContentType == null || "default".equals(messageContentType)) {
            messageContentType = DESCRIPTOR.getDefaultContentType();
            // The defaultContentType is null if the main Hudson configuration
            // was not reconfigured after upgrading.
            if (messageContentType == null) {
                messageContentType = "text/plain";
            }
        }
        messageContentType += "; charset=" + CHARSET;

        msg.setContent(text, messageContentType);
    }

    private static void addAddressesFromRecipientList(List<InternetAddress> addresses, String recipientList,
                                                      EnvVars envVars, BuildListener listener) {
        try {
            List<InternetAddress> internetAddresses = new EmailRecepientUtils().convertRecipientString( recipientList, envVars );
			addresses.addAll(internetAddresses);
		} catch(AddressException ae) {
			LOGGER.log(Level.WARNING, "Could not create email address.", ae);
			listener.getLogger().println("Failed to create e-mail address for " + ae.getRef());
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
}
