package hudson.plugins.emailext.plugins;

import hudson.model.Descriptor;
import java.lang.reflect.Constructor;

import java.util.ArrayList;
import java.util.List;


public abstract class AbstractEmailTriggerDescriptor extends Descriptor<AbstractEmailTrigger> {

    protected List<String> replacesList = new ArrayList<String>();
    protected List<AbstractRecipientProvider> defaultRecipientProviders = new ArrayList<AbstractRecipientProvider>();

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
    
    public void addDefaultRecipientProvider(AbstractRecipientProvider provider) {
        defaultRecipientProviders.add(provider);
    }
    
    public List<AbstractRecipientProvider> getDefaultRecipientProviders() {
        return defaultRecipientProviders;
    }
    
    public abstract AbstractEmailTrigger createDefault();
    
    protected AbstractEmailTrigger _createDefault() {
        AbstractEmailTrigger trigger;
        try {
            Constructor ctor = clazz.getConstructor(List.class, String.class, String.class, String.class, String.class, String.class, int.class, String.class);
            trigger = (AbstractEmailTrigger)ctor.newInstance(defaultRecipientProviders, "", "$PROJECT_DEFAULT_REPLYTO", "$PROJECT_DEFAULT_SUBJECT", "$PROJECT_DEFAULT_CONTENT", "", 0, "project");
        } catch(Exception e) {
                trigger = null;
        }
        return trigger;
    }

    public boolean isWatchable() { return true; }
    
    @Deprecated
    public boolean getDefaultSendToCulprits() {
        return false;
    }
    
    @Deprecated
    public boolean getDefaultSendToDevs() {
        return false;
    }
    
    @Deprecated
    public boolean getDefaultSendToList() {
        return false;
    }
    
    @Deprecated
    public boolean getDefaultSendToRequester() {
        return false;
    }
            }
