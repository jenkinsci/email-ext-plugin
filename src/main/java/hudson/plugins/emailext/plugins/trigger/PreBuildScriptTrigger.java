package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class PreBuildScriptTrigger extends AbstractScriptTrigger {

    public static final String TRIGGER_NAME = "Script (Before Build)";
    
    @DataBoundConstructor
    public PreBuildScriptTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType, triggerScript);
    }
    
    @Override
    public boolean isPreBuild() {
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }
    }
}
