package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import java.util.List;

/**
 * Triggers an email after the specified number of consecutive failures
 * (preceeded by a successful build).
 */
public abstract class NthFailureTrigger extends EmailTrigger {

    @Deprecated
    protected int failureCount;

    /** @deprecated override getRequiredFailureCount instead of passing in failureCount */
    @Deprecated
    public NthFailureTrigger(int failureCount, List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
        this.failureCount = failureCount;
    }

    public NthFailureTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public NthFailureTrigger(int failureCount, boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
        this.failureCount = failureCount;
    }

    @SuppressWarnings("deprecation")
    protected int getRequiredFailureCount() {
        return failureCount;
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        Run<?,?> run = build;
        int count = getRequiredFailureCount();
        // Work back through the failed builds.
        for (int i = 0; i < count; i++) {
            if (run == null) {
                // We don't have enough history to have reached the failure count.
                return false;
            }

            Result buildResult = run.getResult();
            if (buildResult != Result.FAILURE) {
                return false;
            }

            run = ExtendedEmailPublisher.getPreviousRun(run, listener);
        }

        return run == null || run.getResult() == Result.SUCCESS || run.getResult() == Result.UNSTABLE;
    }

    public abstract static class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            addTriggerNameToReplace(FailureTrigger.TRIGGER_NAME);
            addTriggerNameToReplace(StillFailingTrigger.TRIGGER_NAME);
            
            addDefaultRecipientProvider(new DevelopersRecipientProvider());
            addDefaultRecipientProvider(new ListRecipientProvider());
        }
    }
}
