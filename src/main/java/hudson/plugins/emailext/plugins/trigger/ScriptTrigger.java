/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.emailext.plugins.trigger;

import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author acearl
 */
public class ScriptTrigger extends AbstractScriptTrigger {

    public static final String TRIGGER_NAME = "Script Trigger";

    @Override
    public boolean isPreBuild() {
        return false;
    }

   @Override
    public EmailTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    public static class DescriptorImpl extends AbstractScriptTrigger.DescriptorImpl {

        @Override
        public String getTriggerName() {
            return TRIGGER_NAME;
        }

        @Override
        protected EmailTrigger newInstance(StaplerRequest req, JSONObject formData) {
            ScriptTrigger trigger = new ScriptTrigger();
            if(formData != null) {
                trigger.triggerScript = formData.getString("email_ext_scripttrigger_script");
            }
            return trigger;
        }

        @Override
        public String getHelpText() {
            return Messages.ScriptTrigger_HelpText();
        }        
    }
}
