package hudson.plugins.emailext.plugins;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;

public abstract class EmailTrigger {
	
	private EmailType email;

	/**
	 * Implementors of this method need to return true if the conditions to trigger
	 * an email have been met.
	 * @param build The Build object after the project has been built
	 * @return true if the conditions have been met to trigger a build of this type
	 */
	public abstract <P extends AbstractProject<P,B>,B extends AbstractBuild<P,B>>
	boolean trigger(B build);

	/**
	 * Get the email that is with this trigger.
	 * @return the email
	 */
	public EmailType getEmail() {
		return email;
	}

	public void setEmail(EmailType email) {
		if (email == null) {
			email = new EmailType();
			email.setBody(ExtendedEmailPublisher.PROJECT_DEFAULT_BODY_TEXT);
			email.setSubject(ExtendedEmailPublisher.PROJECT_DEFAULT_SUBJECT_TEXT);
		}
		this.email = email;
	}
	
	public abstract EmailTriggerDescriptor getDescriptor();

	public boolean getDefaultSendToList() {
		return false;
	}
	
	public boolean getDefaultSendToDevs() {
		return false;
	}
	
	/**
	 * Should this trigger run before the build?  Defaults to false.
	 */
	public boolean isPreBuild() {
		return false;
	}
}
