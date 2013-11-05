package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.EmailTrigger;

import org.kohsuke.stapler.DataBoundConstructor;

public class StatusChangedTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Status Changed";

    @DataBoundConstructor
    public StatusChangedTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {

        super(sendToList, sendToDevs, sendToRequestor, sendToCulprits, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        final Result buildResult = build.getResult();

        if (buildResult != null) {
            final AbstractBuild<?, ?> prevBuild = ExtendedEmailPublisher.getPreviousBuild(build, listener);

            if (prevBuild == null) {
            	// Notify at the first status defined
            	return true;
            }

            return (build.getResult() != prevBuild.getResult());
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
