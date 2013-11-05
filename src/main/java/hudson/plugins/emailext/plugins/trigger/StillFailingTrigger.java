package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class StillFailingTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Still Failing";
    
    @DataBoundConstructor
    public StillFailingTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequestor, sendToCulprits, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        Result buildResult = build.getResult();

        if (buildResult == Result.FAILURE) {
            AbstractBuild<?, ?> prevBuild = ExtendedEmailPublisher.getPreviousBuild(build, listener);
            if (prevBuild != null && (prevBuild.getResult() == Result.FAILURE)) {
                return true;
            }
        }

        return false;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            //This trigger should send an email in place of the Failure Trigger
            addTriggerNameToReplace(FailureTrigger.TRIGGER_NAME);
        }

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
