package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class ImprovementTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Improvement";
    
    @DataBoundConstructor
    public ImprovementTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequestor, sendToCulprits, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        AbstractBuild<?,?> previousBuild = ExtendedEmailPublisher.getPreviousBuild(build, listener);

        if (previousBuild == null)
            return false;
        if (build.getTestResultAction() == null) return false;
        if (previousBuild.getTestResultAction() == null)
            return false;
        
        int numCurrFailures = getNumFailures(build);

        // The first part of the condition avoids accidental triggering for
        // builds that aggregate downstream test results before those test
        // results are available...
        return build.getTestResultAction().getTotalCount() > 0
                && numCurrFailures < getNumFailures(previousBuild)
                && numCurrFailures > 0;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {
        
        public DescriptorImpl() {
            addTriggerNameToReplace(UnstableTrigger.TRIGGER_NAME);
            addTriggerNameToReplace(StillUnstableTrigger.TRIGGER_NAME);
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
            return true;
        }
    }    
}
