package hudson.plugins.emailext.plugins;

import hudson.model.Descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class EmailTriggerDescriptor extends Descriptor<EmailTrigger> {

    protected List<String> replacesList = new ArrayList<String>();
    protected List<RecipientProvider> recipientProviders = new ArrayList<RecipientProvider>();

    /**
     * You can add the name of a trigger that this trigger should override if both this
     * and the specified trigger meet the criteria to send an email.  If a trigger is
     * specified, then its corresponding email will not be sent.  This is a means to simplify
     * the work a plugin developer needs to do to make sure that only a single email is sent.
     *
     * @param triggerName is the name of a trigger that should be deactivated if it is specified.
     * @see #getTriggerName()
     */
    public void addTriggerNameToReplace(String triggerName) {
        replacesList.add(triggerName);
    }

    public List<String> getTriggerReplaceList() {
        return replacesList;
    }
    
    public void addDefaultRecipientProvider(RecipientProvider provider) {
        recipientProviders.add(provider);
    }
    
    public List<RecipientProvider> getDefaultRecipientProviders() {
        return recipientProviders;
    }
}
