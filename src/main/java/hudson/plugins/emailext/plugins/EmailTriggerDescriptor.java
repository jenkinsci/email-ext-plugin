package hudson.plugins.emailext.plugins;

import hudson.plugins.emailext.EmailType;

import java.util.ArrayList;
import java.util.List;

public abstract class EmailTriggerDescriptor {
	private static final String MAILER_ID_REGEX = "\\s";
	protected List<String> replacesList = new ArrayList<String>();
	
	/**
	 * @return The display name of the trigger type. 
	 */
	public abstract String getTriggerName();
	
	/**
	 * Get a name that can be used to determine which properties in the jelly script
	 * applyt to this trigger.
	 * @return
	 */
	public String getMailerId(){
		return getTriggerName().replaceAll(MAILER_ID_REGEX, "-");
	}
	
	/**
	 * You can add the name of a trigger that this trigger should override if both this 
	 * and the specified trigger meet the criteria to send an email.  If a trigger is 
	 * specified, then its corresponding email will not be sent.  This is a means to simplify
	 * the work a plugin developer needs to do to make sure that only a single email is sent.
	 *  
	 * @param triggerName is the name of a trigger that should be deactivated if it is specified.
	 * @see #getTriggerName()
	 */
	public void addTriggerNameToReplace(String triggerName){
		replacesList.add(triggerName);
	}

	public List<String> getTriggerReplaceList() {
		return replacesList;
	}
	
	protected abstract EmailTrigger newInstance();
	
	public EmailTrigger getNewInstance(EmailType type){
		EmailTrigger trigger = newInstance();
		trigger.setEmail(type);
		return trigger;
	}
	
	public abstract String getHelpText();
}
