package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;


public class AbortedTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Aborted";
    
    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        Result buildResult = build.getResult();

        if (buildResult == Result.ABORTED) {
            return true;
        }

        return false;
    }

    @DataBoundConstructor
    public AbortedTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, boolean sendToUpstream, String recipientList,
                          String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequestor, sendToCulprits, sendToUpstream, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
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
            return true;
        }
    }    
}
