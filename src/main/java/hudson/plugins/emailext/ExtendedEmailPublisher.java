package hudson.plugins.emailext;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Mailer;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * {@link Publisher} that sends notification e-mail.
 *
 * @author kyle.sweeney@valtech.com
 *
 */
public class ExtendedEmailPublisher extends Publisher {
	
	private static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

	public static final String COMMA_SEPARATED_SPLIT_REGEXP = "[,\\s]+";

	private static final String DEFAULT_BODY = "\\$DEFAULT_CONTENT";
	private static final String DEFAULT_SUBJECT = "\\$DEFAULT_SUBJECT";
	
	private static final String PROJECT_DEFAULT_BODY = "\\$PROJECT_DEFAULT_CONTENT";
	private static final String PROJECT_DEFAULT_SUBJECT = "\\$PROJECT_DEFAULT_SUBJECT";
	
	private static final Map<String,EmailContent> EMAIL_CONTENT_TYPE_MAP = new LinkedHashMap<String,EmailContent>();
	private static final Map<String,EmailTriggerDescriptor> EMAIL_TRIGGER_TYPE_MAP = new HashMap<String,EmailTriggerDescriptor>();
	
	public static final String DEFAULT_SUBJECT_TEXT = "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!";
	public static final String DEFAULT_BODY_TEXT = "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:\n\n" +
		"Check console output at $BUILD_URL to view the results.";
	
	public static final String PROJECT_DEFAULT_SUBJECT_TEXT = "$PROJECT_DEFAULT_SUBJECT";
	public static final String PROJECT_DEFAULT_BODY_TEXT = "$PROJECT_DEFAULT_CONTENT";
	
	public static void addEmailContentType(EmailContent contentType) throws EmailExtException {
		if(EMAIL_CONTENT_TYPE_MAP.containsKey(contentType.getToken()))
			throw new EmailExtException("An email content type with token name " +
					contentType.getToken() + " was already added.");
		
		EMAIL_CONTENT_TYPE_MAP.put(contentType.getToken(), contentType);
	}
	
	public static void removeEmailContentType(EmailContent contentType) {
		if(EMAIL_CONTENT_TYPE_MAP.containsKey(contentType.getToken()))
			EMAIL_CONTENT_TYPE_MAP.remove(contentType);
	}
	
	public static EmailContent getEmailContentType(String token) {
		return EMAIL_CONTENT_TYPE_MAP.get(token);
	}

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
		for(String triggerName : EMAIL_TRIGGER_TYPE_MAP.keySet()) {
			retList.add(EMAIL_TRIGGER_TYPE_MAP.get(triggerName).getNewInstance(null));
		}
		return retList;
	}
	
	public static Collection<EmailContent> getEmailContentTypes() {
		return EMAIL_CONTENT_TYPE_MAP.values();
	}
	
	
	/**
	 * A comma-separated list of email recipient that will be used for every trigger.
	 */
	public String recipientList;

	/** This is the list of email triggers that the project has configured */
	private List<EmailTrigger> configuredTriggers = new ArrayList<EmailTrigger>();

	/**
	 * The contentType of the emails for this project.
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
		for(String triggerName : EMAIL_TRIGGER_TYPE_MAP.keySet()) {
			boolean contains = false;
			for(EmailTrigger trigger : confTriggers) {
				if(trigger.getDescriptor().getTriggerName().equals(triggerName)) {
					contains = true;
					break;
				}
			}
			if(!contains) {
				retList.add(EMAIL_TRIGGER_TYPE_MAP.get(triggerName).getNewInstance(null));
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

	@SuppressWarnings("unchecked")
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		return _perform(build,launcher,listener);
	}
	
	public <P extends AbstractProject<P,B>,B extends AbstractBuild<P,B>> boolean _perform(B build, Launcher launcher, BuildListener listener) throws InterruptedException {
	   	boolean emailTriggered = false;
		
	   	Map<String,EmailTrigger> triggered = new HashMap<String, EmailTrigger>();
	   	
		for(EmailTrigger trigger : configuredTriggers) {
			if(trigger.trigger(build)) {
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
		
		listener.getLogger().println("There are " + triggered.size() + " triggered emails.");
		for(String triggerName :triggered.keySet()) {
			listener.getLogger().println("Sending email for trigger: " + triggerName);
			sendMail(triggered.get(triggerName).getEmail(), build, listener);
		}
		
		return true;
	}
	
	public <P extends AbstractProject<P,B>,B extends AbstractBuild<P,B>>
	boolean sendMail(EmailType mailType, B build, BuildListener listener) {
		try {
			MimeMessage msg = createMail(mailType, build, listener);
			Address[] allRecipients = msg.getAllRecipients();
			if (allRecipients != null) {
				StringBuffer buf = new StringBuffer("Sending e-mails to:");
				for (Address a : allRecipients)
					buf.append(' ').append(a);
				listener.getLogger().println(buf);
				Transport.send(msg);
				return true;
			} else {
				listener.getLogger().println("An attempt to send an e-mail"
					+ " to empty list of recipients, ignored.");
			}
		} catch(MessagingException e) {
			LOGGER.log(Level.WARNING, "Could not send email.",e);
			e.printStackTrace(listener.error("Could not send email as a part of the post-build publishers."));
		}
		
		return false;
	}

	private <P extends AbstractProject<P,B>,B extends AbstractBuild<P,B>>
	MimeMessage createMail(EmailType type, B build, BuildListener listener) throws MessagingException {
		MimeMessage msg = new MimeMessage(ExtendedEmailPublisher.DESCRIPTOR.createSession());

		//Set the contents of the email
		msg.setFrom(new InternetAddress(ExtendedEmailPublisher.DESCRIPTOR.getAdminAddress()));
		msg.setSentDate(new Date());
		String subject = transformText(type,type.getSubject(),build);
		msg.setSubject(subject);
		String text = transformText(type,type.getBody(),build);
		msg.setContent(text, contentType);
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
		msg.setContent(text, messageContentType);

		// Get the recipients from the global list of addresses
		List<InternetAddress> recipientAddresses = new ArrayList<InternetAddress>();
		if (type.getSendToRecipientList()) {
			for (String recipient : recipientList.split(COMMA_SEPARATED_SPLIT_REGEXP)) {
				addAddress(recipientAddresses, recipient, listener);
			}
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
					addAddress(recipientAddresses, adrs, listener);
				else {
					listener.getLogger().println("Failed to send e-mail to " + user.getFullName() + " because no e-mail address is known, and no default e-mail domain is configured");
				}
			}
		}
		//Get the list of recipients that are uniquely specified for this type of email 
		if (type.getRecipientList() != null && type.getRecipientList().trim().length() > 0) {
			String[] typeRecipients = type.getRecipientList().split(COMMA_SEPARATED_SPLIT_REGEXP);
			for (int i = 0; i < typeRecipients.length; i++) {
				recipientAddresses.add(new InternetAddress(typeRecipients[i]));
			}
		}
		
		msg.setRecipients(Message.RecipientType.TO, recipientAddresses.toArray(new InternetAddress[recipientAddresses.size()]));
		return msg;
	}

	private static void addAddress(List<InternetAddress> addresses, String address, BuildListener listener) {
		try {
			addresses.add(new InternetAddress(address));
		} catch(AddressException ae) {
			LOGGER.log(Level.WARNING, "Could not create email address.", ae);
			listener.getLogger().println("Failed to create e-mail address for " + address);
		}
	}
	
	private <P extends AbstractProject<P,B>,B extends AbstractBuild<P,B>> String transformText(EmailType type,String origText,B build) {

		String newText = origText.replaceAll(PROJECT_DEFAULT_BODY, Matcher.quoteReplacement(defaultContent))
		 						 .replaceAll(PROJECT_DEFAULT_SUBJECT, Matcher.quoteReplacement(defaultSubject))
								 .replaceAll(DEFAULT_BODY, Matcher.quoteReplacement(DESCRIPTOR.getDefaultBody()))
								 .replaceAll(DEFAULT_SUBJECT, Matcher.quoteReplacement(DESCRIPTOR.getDefaultSubject()));
						
		newText = replaceTokensWithContent(newText, type, build);
		return newText;
	}
	
	private static <P extends AbstractProject<P,B>,B extends AbstractBuild<P,B>> String replaceTokensWithContent(String origText,EmailType type,AbstractBuild<P,B> build) {
		StringBuffer sb = new StringBuffer();
		Tokenizer tokenizer = new Tokenizer(origText);

		while (tokenizer.find()) {
			String tokenName = tokenizer.getTokenName();
			Map<String, Object> args = tokenizer.getArgs();
			EmailContent content = EMAIL_CONTENT_TYPE_MAP.get(tokenName);
			String replacement;
			if (content != null) {
				try {
					replacement = content.getContent(build, type, args);
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,
							"Exception thrown while replacing " + tokenizer.group(),
							e);
					replacement = "[[ Exception while replacing " + tokenName + ".  Please report this as a bug. ]]";
				}
				if (content.hasNestedContent()) {
					replacement = replaceTokensWithContent(replacement, type, build);
				}
			} else {
				replacement = tokenizer.group();
			}
			tokenizer.appendReplacement(sb, replacement);
		}
		tokenizer.appendTail(sb);
		
		return sb.toString();
	}

	static class Tokenizer {
		private static final String tokenNameRegex = "[a-zA-Z0-9_]+";
		
		private static final String numberRegex = "-?[0-9]+(\\.[0-9]*)?";
		private static final String boolRegex = "(true)|(false)";
		// Sequence of (1) not \ " CR LF and (2) \ followed by non line terminator
		private static final String stringRegex = "\"([^\\\\\"\\r\\n]|(\\\\.))*\"";
		private static final String valueRegex = "(" + numberRegex + ")|(" + boolRegex + ")|(" + stringRegex + ")";
		
		private static final String spaceRegex = "[ \\t]*";
		private static final String argRegex = "(" + tokenNameRegex + ")" + spaceRegex + "=" + spaceRegex + "(" + valueRegex + ")";
		private static final String argsRegex = "((" + spaceRegex + "," + spaceRegex + argRegex + ")*)";
		
		private static final String delimitedTokenRegex = "\\{" + spaceRegex + "(" + tokenNameRegex + ")" + argsRegex + spaceRegex + "\\}";
		private static final String tokenRegex = "\\$((" + tokenNameRegex + ")|(" + delimitedTokenRegex + "))";
		
		private static final Pattern argPattern = Pattern.compile(argRegex);
		private static final Pattern tokenPattern = Pattern.compile(tokenRegex);
		
		private final Matcher tokenMatcher;
		private String tokenName = null;
		private Map<String, Object> args = null;
		
		Tokenizer(String origText) {
			tokenMatcher = tokenPattern.matcher(origText);
		}
		
		String getTokenName() {
			return tokenName;
		}
		
		Map<String, Object> getArgs() {
			return args;
		}
		
		String group() {
			return tokenMatcher.group();
		}
		
		boolean find() {
			if (tokenMatcher.find()) {
				tokenName = tokenMatcher.group(2);
				if (tokenName == null) {
					tokenName = tokenMatcher.group(4);
				}
				args = new HashMap<String, Object>();
				if (tokenMatcher.group(5) != null) {
					parseArgs(tokenMatcher.group(5), args);
				}
				return true;
			} else {
				return false;
			}
		}
		
		static void parseArgs(String argsString, Map<String, Object> args) {
			Matcher argMatcher = argPattern.matcher(argsString);
			while (argMatcher.find()) {
				Object arg;
				if (argMatcher.group(3) != null) {
					// number
					if (argMatcher.group(4) != null) {
						arg = Float.valueOf(argMatcher.group(3));
					} else {
						arg = Integer.valueOf(argMatcher.group(3));
					}
				} else if (argMatcher.group(5) != null) {
					// boolean
					if (argMatcher.group(6) != null) {
						arg = Boolean.TRUE;
					} else {
						arg = Boolean.FALSE;
					}
				} else { // if (argMatcher.group(8) != null) {
					// string
					arg = Util.unescapeString(argMatcher.group(8));
				}
				args.put(argMatcher.group(1), arg);
			}
		}
		
		void appendReplacement(StringBuffer sb, String replacement) {
			tokenMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		
		void appendTail(StringBuffer sb) {
			tokenMatcher.appendTail(sb);
		}
		
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}
	
	public Descriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}
	
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	/*
	 * These settings are the settings that are global.
	 */
	public static final class DescriptorImpl extends Descriptor<Publisher> {
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
		 * If non-null, use SMTP-AUTH with these information.
		 */
		private String smtpAuthPassword,smtpAuthUsername;

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

		@Override
		public String getDisplayName() {
			return "Editable Email Notification";
		}
		
		public String getAdminAddress() {
			String v = adminAddress;
			if (v == null) {
				v = "address not configured yet <nobody>";
			}
			return v;
		}

		public String getDefaultSuffix() {
			return defaultSuffix;
		}
		
		/** JavaMail session. */
		public Session createSession() {
			Properties props = new Properties(System.getProperties());
			if(smtpHost!=null)
				props.put("mail.smtp.host",smtpHost);
			if (smtpPort!=null) {
				props.put("mail.smtp.port", smtpPort);
			}
			if (useSsl) {
				/* This allows the user to override settings by setting system properties but
				 * also allows us to use the default SMTPs port of 465 if no port is already set.
				 * It would be cleaner to use smtps, but that's done by calling session.getTransport()...
				 * and thats done in mail sender, and it would be a bit of a hack to get it all to
				 * coordinate, and we can make it work through setting mail.smtp properties.
				 */
				props.put("mail.smtp.auth","true");
				if (props.getProperty("mail.smtp.socketFactory.port") == null) {
					String port = smtpPort==null?"465":smtpPort;
					props.put("mail.smtp.port", port);
					props.put("mail.smtp.socketFactory.port", port);
				}
				if (props.getProperty("mail.smtp.socketFactory.class") == null) {
					props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
				}
				props.put("mail.smtp.socketFactory.fallback", "false");
			}
			return Session.getInstance(props,getAuthenticator());
		}
		
		private Authenticator getAuthenticator() {
			final String un = getSmtpAuthUsername();
			if (un == null) return null;
			return new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(getSmtpAuthUsername(), getSmtpAuthPassword());
				}
			};
		}

		public String getHudsonUrl() {
			if (hudsonUrl == null) {
				return Hudson.getInstance().getRootUrl();
			}
			return hudsonUrl;
		}

		public String getSmtpServer() {
			return smtpHost;
		}

		public String getSmtpAuthUsername() {
			return smtpAuthUsername;
		}
		
		public String getSmtpAuthPassword() {
			return smtpAuthPassword;
		}

		public boolean getUseSsl() {
			return useSsl;
		}
		
		public String getSmtpPort() {
			return smtpPort;
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

		@Override
		public Publisher newInstance(StaplerRequest req) throws hudson.model.Descriptor.FormException {
			// Save the recipient lists
			String listRecipients = req.getParameter("recipientlist_recipients");
			
			// Save configuration for each trigger type
			ExtendedEmailPublisher m = new ExtendedEmailPublisher();
			m.recipientList = listRecipients;
			m.contentType = req.getParameter("project_content_type");
			m.defaultSubject = req.getParameter("project_default_subject");
			m.defaultContent = req.getParameter("project_default_content");
			m.configuredTriggers = new ArrayList<EmailTrigger>();
			
			// Create a new email trigger for each one that is configured
			for (String mailerId : EMAIL_TRIGGER_TYPE_MAP.keySet()) {
				EmailType type = createMailType(req, mailerId);
				if (type != null) {
					EmailTrigger trigger = EMAIL_TRIGGER_TYPE_MAP.get(mailerId).getNewInstance(type);
					m.configuredTriggers.add(trigger);
				}
			}
			
			req.bindParameters(m, "ext_mailer_");
			return m;
		}
		
		private EmailType createMailType(StaplerRequest req, String mailType) {
			if(req.getParameter("mailer." + mailType + ".configured") == null)
				return null;
			if(!req.getParameter("mailer." + mailType + ".configured").equalsIgnoreCase("true"))
				return null;
			
			EmailType m = new EmailType();
			String prefix = "mailer." + mailType + ".";
			m.setSubject(req.getParameter(prefix + "subject"));
			m.setBody(req.getParameter(prefix + "body"));
			m.setRecipientList(req.getParameter(prefix + "recipientList"));
			m.setSendToRecipientList(req.getParameter(prefix + "sendToRecipientList")!=null);
			m.setSendToDevelopers(req.getParameter(prefix + "sendToDevelopers")!=null);
			m.setIncludeCulprits(req.getParameter(prefix + "includeCulprits")!=null);
			return m;
		}
		
		public DescriptorImpl() {
			super(ExtendedEmailPublisher.class);
			load();
			if (defaultBody == null && defaultSubject == null) {
				defaultBody = DEFAULT_BODY_TEXT;
				defaultSubject = DEFAULT_SUBJECT_TEXT;
			}
		}

		@Override
		public boolean configure(StaplerRequest req) throws FormException {
			// Most of this stuff is the same as the built-in email publisher

			// Configure the smtp server
			smtpHost = nullify(req.getParameter("ext_mailer_smtp_server"));
			adminAddress = req.getParameter("ext_mailer_admin_address");
			defaultSuffix = nullify(req.getParameter("ext_mailer_default_suffix"));
			
			// Specify the url to this hudson instance
			String url = nullify(req.getParameter("ext_mailer_hudson_url"));
			if (url != null && !url.endsWith("/")) {
				url += '/';
			}
			if (url == null) {
				url = Hudson.getInstance().getRootUrl();
			}
			hudsonUrl = url;

			// specify authentication information
			if (req.getParameter("extmailer.useSMTPAuth") != null) {
				smtpAuthUsername = nullify(req.getParameter("extmailer.SMTPAuth.userName"));
				smtpAuthPassword = nullify(req.getParameter("extmailer.SMTPAuth.password"));
			} else {
				smtpAuthUsername = smtpAuthPassword = null;
			}
			
			// specify if the mail server uses ssl for authentication
			useSsl = req.getParameter("ext_mailer_smtp_use_ssl") != null;
			
			// specify custom smtp port
			smtpPort = nullify(req.getParameter("ext_mailer_smtp_port"));
			
			defaultContentType = nullify(req.getParameter("ext_mailer_default_content_type"));

			// Allow global defaults to be set for the subject and body of the email
			defaultSubject = nullify(req.getParameter("ext_mailer_default_subject"));
			defaultBody = nullify(req.getParameter("ext_mailer_default_body"));
			
			save();
			return super.configure(req);
		}
		
		private String nullify(String v) {
			if(v!=null && v.length()==0)	
				v=null;
			return v;
		}
		
		@Override
		public String getHelpFile() {
			return "/plugin/email-ext/help/main.html";
		}
		
		public void doAddressCheck(StaplerRequest req, StaplerResponse rsp,
				@QueryParameter("value") final String value) throws IOException, ServletException {
			new FormFieldValidator(req,rsp,false) {
				protected void check() throws IOException, ServletException {
					try {
						new InternetAddress(value);
						ok();
					} catch (AddressException e) {
						error(e.getMessage());
					}
				}
			}.process();
		}
		
		public void doRecipientListRecipientsCheck(StaplerRequest req, StaplerResponse rsp,
				@QueryParameter("value") final String value) throws IOException, ServletException {
			new FormFieldValidator(req,rsp,false) {
				protected void check() throws IOException, ServletException {
					if(value != null && value.trim().length() > 0) {
						String[] names = value.split(COMMA_SEPARATED_SPLIT_REGEXP);
						try {
							for(int i=0;i<names.length;i++) {
								if(names[i].trim().length()>0) {
									new InternetAddress(names[i]);
								}
							}
							ok();
						}
						catch(AddressException e) {
							error(e.getMessage());
						}
					}
					else
						ok();
				}
			}.process();
		}
		
	}



}
