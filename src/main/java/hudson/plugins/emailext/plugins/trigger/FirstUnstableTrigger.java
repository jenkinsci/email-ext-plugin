package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 */
public class FirstUnstableTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "1st Unstable";

    @DataBoundConstructor
    public FirstUnstableTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, boolean sendToUpstream,
                                String recipientList, String replyTo, String subject, String body,
                                String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequestor, sendToCulprits, sendToUpstream, recipientList, replyTo, subject, body,
                attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        AbstractBuild<?, ?> previousBuild = ExtendedEmailPublisher.getPreviousBuild(build, listener);
        return previousBuild != null ?
                previousBuild.getResult() != Result.UNSTABLE && build.getResult() == Result.UNSTABLE :
                build.getResult() == Result.UNSTABLE;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            addTriggerNameToReplace(UnstableTrigger.TRIGGER_NAME);
            addTriggerNameToReplace(StatusChangedTrigger.TRIGGER_NAME);
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
