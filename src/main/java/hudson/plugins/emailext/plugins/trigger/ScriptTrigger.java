package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class ScriptTrigger extends AbstractScriptTrigger {

    public static final String TRIGGER_NAME = "Script (After Build)";
    
    @DataBoundConstructor
    public ScriptTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType, triggerScript);
    }

    @Override
    public boolean isPreBuild() {
        return false;
    }

    @Extension
    public static class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            addDefaultRecipientProvider(new ListRecipientProvider());
        }
        
        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }
    }    
}
