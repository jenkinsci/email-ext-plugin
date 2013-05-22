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
public class PreBuildScriptTrigger extends AbstractScriptTrigger {

    public static final String TRIGGER_NAME = "Pre-Build Script Trigger";
    
    @Override
    public boolean isPreBuild() {
        return true;
    }

    @Override
    public EmailTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractScriptTrigger.DescriptorImpl {

        @Override
        public String getTriggerName() {
            return TRIGGER_NAME;
        }

        @Override
        public EmailTrigger newInstance(StaplerRequest req, JSONObject formData) {
            PreBuildScriptTrigger trigger = new PreBuildScriptTrigger();
            if(formData != null) {
              trigger.triggerScript = formData.getString("email_ext_prebuildscripttrigger_script");
            }
            return trigger;
        }

        @Override
        public String getHelpText() {
            return Messages.PreBuildScriptTrigger_HelpText();
        }
    }
}
