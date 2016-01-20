package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.AbstractEmailTrigger;
import hudson.plugins.emailext.plugins.AbstractEmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.AbstractRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.tasks.test.AbstractTestResultAction;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class ImprovementTrigger extends AbstractEmailTrigger {

    public static final String TRIGGER_NAME = "Test Improvement";
    
    @DataBoundConstructor
    public ImprovementTrigger(List<AbstractRecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public ImprovementTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        Run<?,?> previousRun = ExtendedEmailPublisher.getPreviousRun(build, listener);

        if (previousRun == null)
            return false;
        if (build.getAction(AbstractTestResultAction.class) == null) return false;
        if (previousRun.getAction(AbstractTestResultAction.class) == null)
            return false;
        
        int numCurrFailures = getNumFailures(build);

        // The first part of the condition avoids accidental triggering for
        // builds that aggregate downstream test results before those test
        // results are available...
        return build.getAction(AbstractTestResultAction.class).getTotalCount() > 0
                && numCurrFailures < getNumFailures(previousRun)
                && numCurrFailures > 0;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractEmailTriggerDescriptor {
        
        public DescriptorImpl() {
            addTriggerNameToReplace(UnstableTrigger.TRIGGER_NAME);
            addTriggerNameToReplace(StillUnstableTrigger.TRIGGER_NAME);
            
            addDefaultRecipientProvider(new DevelopersRecipientProvider());
            addDefaultRecipientProvider(new ListRecipientProvider());
        }
        
        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }
        
        @Override
        public AbstractEmailTrigger createDefault() {
            return _createDefault();
        }
    }    
}
