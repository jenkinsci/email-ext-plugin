package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;


public class FailureTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Failure";
    
    public static FailureTrigger createDefault() {
        return new FailureTrigger(false, true, true, true, "", "$PROJECT_DEFAULT_REPLYTO", "$PROJECT_DEFAULT_SUBJECT", "$PROJECT_DEFAULT_CONTENT", "", 0, "project");
    }
    
    @DataBoundConstructor
    public FailureTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequestor, sendToCulprits, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        Result buildResult = build.getResult();
        if (buildResult == Result.FAILURE) {
            return true;
        }
        return false;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }

        @Override
        public boolean getDefaultSendToDevs() {
            return true;
        }

        @Override
        public boolean getDefaultSendToList() {
            return false;
        }
    }    
}
