package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class PreBuildScriptTrigger extends AbstractScriptTrigger {

    public static final String TRIGGER_NAME = "Script - Before Build";
    
    @DataBoundConstructor
    public PreBuildScriptTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType, triggerScript);
    }
    
    @Deprecated
    public PreBuildScriptTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType, triggerScript);
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

        @Override
        public boolean isWatchable() {
            return false;
        }
        
        @Override
        public EmailTrigger createDefault() {
            return new PreBuildScriptTrigger(defaultRecipientProviders, "", "$PROJECT_DEFAULT_REPLYTO", "$PROJECT_DEFAULT_SUBJECT", "$PROJECT_DEFAULT_CONTENT", "", 0, "project", "");
        }
    }
}
