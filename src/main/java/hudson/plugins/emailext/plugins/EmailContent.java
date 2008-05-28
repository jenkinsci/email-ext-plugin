package hudson.plugins.emailext.plugins;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Project;
import hudson.plugins.emailext.EmailType;

public interface EmailContent {
	
	/**
	 * This is the token that will be replaced by the content when the email is sent.
	 * If the email has a string like "$REPLACE_ME", then the implementation of this 
	 * method should return "REPLACE_ME".
	 */
	public String getToken();
	
	/**
	 * This method returns the generated content that should replace the token.
	 */
	public <P extends AbstractProject<P,B>, B extends AbstractBuild<P,B>> String getContent(AbstractBuild<P, B> build, EmailType emailType);

	/**
	 * Specifies whether or not the content returned by this object can have nested
	 * tokens in it that need to be resolved before sending the email.
	 */
	public boolean hasNestedContent();
	
	/**
	 * This is a string that will be rendered in the help section of the plugin.
	 * It describes what the content does and what it puts in the email.
	 */
	public String getHelpText();
}
