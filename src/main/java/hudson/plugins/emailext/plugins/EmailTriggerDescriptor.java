package hudson.plugins.emailext.plugins;

import hudson.model.Descriptor;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public abstract class EmailTriggerDescriptor extends Descriptor<EmailTrigger> {

    protected List<String> replacesList = new ArrayList<String>();

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
    
    public boolean getDefaultSendToList() {
        return false;
    }

    public boolean getDefaultSendToDevs() {
        return false;
    }

    public boolean getDefaultSendToRequester() {
        return false;
    }
    
    public boolean getDefaultSendToCulprits() {
        return false;
    }

    /**
     * Default implementation just creates a new instance of the 
     * trigger class and returns that.
     */
    @Override
    public EmailTrigger newInstance(StaplerRequest req, JSONObject formData) {
        EmailTrigger res = null;
        try {
            res = clazz.newInstance();
            res.configure(req, formData);
        } catch(Exception e) {
            // should do something here?
        }
        return res;
    }
}
