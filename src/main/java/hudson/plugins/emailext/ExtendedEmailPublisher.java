package hudson.plugins.emailext;

import hudson.Launcher;
import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Builder;
import hudson.tasks.Mailer;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

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
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link ExtendedEmailPublisher} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(Build, Launcher, BuildListener)} method
 * will be invoked. This Publisher seeks to solve some of the issues people are having with
 * Hudson's default email publisher.  Mainly that you cannot send emails on a successful 
 * build.  This plugin should allow you to send any combination of email based on build
 * status and recipients.
 * 
 * @author kyle.sweeney@valtech.com
 *
 */
public class ExtendedEmailPublisher extends Publisher {
	
	private static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

    /**
     * @see #extractAddressFromId(String)
     */
    public static final String EMAIL_ADDRESS_REGEXP = "^.*<([^>]+)>.*$";
    
    public static final String COMMA_SEPARATED_SPLIT_REGEXP = "[,\\s]+";

	private static final String PROJECT_NAME = "\\$PROJECT_NAME";
	private static final String BUILD_NUMBER = "\\$BUILD_NUMBER";
	private static final String BUILD_STATUS = "\\$BUILD_STATUS";
	private static final String BUILD_URL = "\\$BUILD_URL";
	private static final String PROJECT_URL = "\\$PROJECT_URL";
	private static final String HUDSON_URL = "\\$HUDSON_URL";
	private static final String CHANGES = "$CHANGES";
	private static final String BUILD_LOG = "$BUILD_LOG";
	private static final String DEFAULT_BODY = "\\$DEFAULT_CONTENT";
	private static final String DEFAULT_SUBJECT = "\\$DEFAULT_SUBJECT";
	
	private static final String DEFAULT_SUBJECT_TEXT = "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!";
	private static final String DEFAULT_BODY_TEXT = "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:\n\n" +
		"Check console output at $HUDSON_URL/$BUILD_URL to view the results.";
    
    /**
     * Email to send when a build has a fatal error
     */
    public EmailType failureMail;
    /**
     * Email to send when the build status changes from stable to unstable
     */
    public EmailType unstableMail;
    /**
     * Email to send when the build status is still failing
     */
    public EmailType stillFailingMail;
    /**
     * Email to send when the is still unstable
     */
    public EmailType stillUnstableMail;
    /**
     * Email to send when the build status changes from unstable or failing to stable/successful
     */
    public EmailType fixedMail;
    /**
     * Email to send when the build status is still stable/successful
     */
    public EmailType successfulMail;

    
    /**
     * An array of email recipient lists.  These can be configured to be used for each email type.
     */
    public String recipientList;
    
	public boolean perform(Build build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		return _perform(build,launcher,listener);
	}
	
    public <P extends Project<P,B>,B extends Build<P,B>> boolean _perform(B build, Launcher launcher, BuildListener listener) throws InterruptedException {
    	Result buildResult = build.getResult();
    	boolean succeeded = false;
    	EmailType type= null;
        if(buildResult == Result.FAILURE){
        	B prevBuild = build.getPreviousBuild();
	    	if(prevBuild!=null && (prevBuild.getResult() == Result.FAILURE))
	    		type = stillFailingMail;
	    	else
	        	type = failureMail;
        }
        else if(buildResult == Result.UNSTABLE){
        	B prevBuild = build.getPreviousBuild();
        	if(prevBuild!=null && (prevBuild.getResult() == Result.UNSTABLE))
        		type = stillUnstableMail;
        	else
        		type = unstableMail;
        }
        else if(buildResult == Result.SUCCESS){
        	B prevBuild = build.getPreviousBuild();
        	if(prevBuild!=null && (prevBuild.getResult() == Result.UNSTABLE || prevBuild.getResult() == Result.FAILURE))
        		type = fixedMail;
        	else
        		type = successfulMail;
        }
                	
        if(type != null){
        	if(type.getHasRecipients())
        		succeeded = sendMail(type,build,listener);
        	else{
        		succeeded = true;
        		listener.getLogger().println("There are no recipients configured for this type of email, so no email will be sent.");
        	}
        }
        
    	return succeeded;
    }
    
    private <P extends Project<P,B>,B extends Build<P,B>> boolean sendMail(EmailType mailType,B build, BuildListener listener){
    	try{
    		MimeMessage msg = createMail(mailType,build,listener);
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
    	}catch(MessagingException e){
    		LOGGER.log(Level.WARNING, "Could not send email.");
    		listener.getLogger().println("Could not send email as a part of the post-build publishers.");
    	}
    	
    	return false;
    }

    private <P extends Project<P,B>,B extends Build<P,B>> MimeMessage createMail(EmailType type,B build,BuildListener listener) throws MessagingException {
        MimeMessage msg = new MimeMessage(ExtendedEmailPublisher.DESCRIPTOR.createSession());

        //Set the contents of the email
        msg.setContent("", "text/plain");
        msg.setFrom(new InternetAddress(ExtendedEmailPublisher.DESCRIPTOR.getAdminAddress()));
        msg.setSentDate(new Date());
        String subject = transformText(type,type.getSubject(),build);
        msg.setSubject(subject);
        String text = transformText(type,type.getBody(),build);
        msg.setText(text);

        //Get the recipients from the global list of addresses
        List<InternetAddress> rcp = new ArrayList<InternetAddress>();
        if (type.getSendToRecipientList()){
        	String[] recipients = recipientList.split(COMMA_SEPARATED_SPLIT_REGEXP);
        	for(int i=0;i<recipients.length;i++){
        		rcp.add(new InternetAddress(recipients[i]));
        	}
        }
        //Get the list of developers who made changes between this build and the last
        //if this mail type is configured that way
        if (type.getSendToDevelopers()) {
            Set<User> users = new HashSet<User>();
            for (Entry change : build.getChangeSet()) {
                User a = change.getAuthor();
                if (users.add(a)) {
                    String adrs = a.getProperty(Mailer.UserProperty.class).getAddress();
                    if (adrs != null)
                        rcp.add(new InternetAddress(adrs));
                    else {
                        listener.getLogger().println("Failed to send e-mail to " + a.getFullName() + " because no e-mail address is known, and no default e-mail domain is configured");
                    }
                }
            }
        }
        //Get the list of recipients that are uniquely specified for this type of email 
        if(type.getRecipientList() != null && type.getRecipientList().trim().length()>0){
	        String[] typeRecipients = type.getRecipientList().split(COMMA_SEPARATED_SPLIT_REGEXP);
	        for(int i=0;i<typeRecipients.length;i++)
	        	rcp.add(new InternetAddress(typeRecipients[i]));
        }
        
        msg.setRecipients(Message.RecipientType.TO, rcp.toArray(new InternetAddress[rcp.size()]));
        return msg;
    }
    
    private <P extends Project<P,B>,B extends Build<P,B>> String transformText(EmailType type,String origText,B build){
    	String status = getEmailTypeAsString(type);
 
    	String changes = "";
    	String log = "Not implemented yet.";

    	if(origText.indexOf(CHANGES)>=0)
    		changes = getChangesSinceLastBuild(build);
    	
    	String newText = origText.replaceAll(DEFAULT_BODY, Matcher.quoteReplacement(DESCRIPTOR.getDefaultBody()))
    							 .replaceAll(DEFAULT_SUBJECT, Matcher.quoteReplacement(DESCRIPTOR.getDefaultSubject()))
    							 .replaceAll(PROJECT_NAME, build.getProject().getName())
    							 .replaceAll(PROJECT_URL, Util.encode(build.getProject().getUrl()))
    							 .replaceAll(BUILD_NUMBER, ""+build.getNumber())
    							 .replaceAll(BUILD_STATUS, status)
    							 .replaceAll(BUILD_LOG, log)
    							 .replaceAll(CHANGES, changes)
    							 .replaceAll(BUILD_URL, Util.encode(build.getUrl()))
    							 .replaceAll(HUDSON_URL, Util.encode(DESCRIPTOR.hudsonUrl));
    	return newText;
    }

	private <P extends Project<P,B>,B extends Build<P,B>> String getChangesSinceLastBuild(B build) {
    	StringBuffer buf= new StringBuffer();
        for (ChangeLogSet.Entry entry : build.getChangeSet()) {
            buf.append('[');
            buf.append(entry.getAuthor().getFullName());
            buf.append("] ");
            String m = entry.getMsg();
            buf.append(m);
            if (!m.endsWith("\n")) {
                buf.append('\n');
            }
            buf.append('\n');
        }
        return buf.toString();
	}
    
    

	private String getEmailTypeAsString(EmailType type) {
		String status;
		if(type==successfulMail)
    		status = "Successful";
    	else if(type==stillUnstableMail)
    		status = "Still Unstable";
    	else if(type==failureMail)
    		status = "Failed";
    	else if(type==fixedMail)
    		status = "Fixed";
    	else if(type==stillFailingMail)
    		status = "Still Failing";
    	else if(type==unstableMail)
    		status = "Unstable";
    	else
    		status = "Unknown";
		return status;
	}

    public Descriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}
    
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

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
         * If true use SSL on port 465 (standard SMTPS).
         */
        private boolean useSsl;
        
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
            if(v==null)
            	v = "address not configured yet <nobody>";
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
            if (useSsl) {
            	/* This allows the user to override settings by setting system properties but
            	 * also allows us to use the default SMTPs port of 465 if no port is already set.
            	 * It would be cleaner to use smtps, but that's done by calling session.getTransport()...
            	 * and thats done in mail sender, and it would be a bit of a hack to get it all to
            	 * coordinate, and we can make it work through setting mail.smtp properties.
            	 */
            	props.put("mail.smtp.auth","true");
            	if (props.getProperty("mail.smtp.socketFactory.port") == null) {
				    props.put("mail.smtp.port", "465");
    				props.put("mail.smtp.socketFactory.port", "465");
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
            if(un==null)    return null;
            return new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(getSmtpAuthUsername(),getSmtpAuthPassword());
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
			return smtpAuthPassword;
		}

		public boolean getUseSsl() {
			return useSsl;
		}
		
		public String getDefaultBody() {
			return defaultBody;
		}

		public String getDefaultSubject() {
			return defaultSubject;
		}

		@Override
		public Publisher newInstance(StaplerRequest req) throws hudson.model.Descriptor.FormException {
			//Save the recipient lists
			String listRecipients = req.getParameter("recipientlist_recipients");
			
			//Save configuration for each build type
			ExtendedEmailPublisher m = new ExtendedEmailPublisher();
			m.recipientList = listRecipients;
			m.unstableMail = createMailType(req,"unstableMail");
			m.failureMail = createMailType(req,"failureMail");
			m.stillFailingMail = createMailType(req,"stillFailingMail");
			m.fixedMail = createMailType(req,"fixedMail");
			m.successfulMail = createMailType(req,"successfulMail");
			m.stillUnstableMail = createMailType(req,"stillUnstableMail");

			req.bindParameters(m,"ext_mailer_");
			return m;
		}
		
		private EmailType createMailType(StaplerRequest req, String mailType){
			EmailType m = new EmailType();
			String prefix = "mailer." + mailType + ".";
			m.setSubject(req.getParameter(prefix + "subject"));
			m.setBody(req.getParameter(prefix + "body"));
			m.setRecipientList(req.getParameter(prefix + "recipientList"));
			m.setSendToRecipientList(req.getParameter(prefix + "sendToRecipientList")!=null);
			m.setSendToDevelopers(req.getParameter(prefix + "sendToDevelopers")!=null);
			return m;
		}
		
		public DescriptorImpl() {
            super(ExtendedEmailPublisher.class);
            load();
            if(defaultBody == null && defaultSubject == null){
            	defaultBody = DEFAULT_BODY_TEXT;
            	defaultSubject = DEFAULT_SUBJECT_TEXT;
            }
        }

		@Override
		public boolean configure(StaplerRequest req) throws FormException {
			//Most of this stuff is the same as the built-in email publisher
			//Configure the smtp server
            smtpHost = nullify(req.getParameter("ext_mailer_smtp_server"));
            adminAddress = req.getParameter("ext_mailer_admin_address");
            defaultSuffix = nullify(req.getParameter("ext_mailer_default_suffix"));
            
            //Specify the url to this hudson instance
            String url = nullify(req.getParameter("ext_mailer_hudson_url"));
            if(url!=null && !url.endsWith("/"))
                url += '/';
            hudsonUrl = url;

            //specify authentication information
            if(req.getParameter("extmailer.useSMTPAuth")!=null) {
                smtpAuthUsername = nullify(req.getParameter("extmailer.SMTPAuth.userName"));
                smtpAuthPassword = nullify(req.getParameter("extmailer.SMTPAuth.password"));
            } else {
                smtpAuthUsername = smtpAuthPassword = null;
            }
            
            //specify if the mail server uses ssl for authentication
            useSsl = req.getParameter("ext_mailer_smtp_use_ssl")!=null;
            
            //Allow global defaults to be set for the subject and body of the email
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
            return "/plugin/email-ext/help-config.html";
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
            		if(value != null && value.trim().length() > 0){
            			String[] names = value.split(COMMA_SEPARATED_SPLIT_REGEXP);
            			try{
            				for(int i=0;i<names.length;i++){
            					if(names[i].trim().length()>0){
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
