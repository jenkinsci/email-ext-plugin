package hudson.plugins.emailext.plugins.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
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
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class RegressionTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Test Regression";

    @DataBoundConstructor
    public RegressionTrigger(
            List<RecipientProvider> recipientProviders,
            String recipientList,
            String replyTo,
            String subject,
            String body,
            String attachmentsPattern,
            int attachBuildLog,
            String contentType) {
        super(
                recipientProviders,
                recipientList,
                replyTo,
                subject,
                body,
                attachmentsPattern,
                attachBuildLog,
                contentType);
    }

    @Deprecated
    public RegressionTrigger(
            boolean sendToList,
            boolean sendToDevs,
            boolean sendToRequester,
            boolean sendToCulprits,
            String recipientList,
            String replyTo,
            String subject,
            String body,
            String attachmentsPattern,
            int attachBuildLog,
            String contentType) {
        super(
                sendToList,
                sendToDevs,
                sendToRequester,
                sendToCulprits,
                recipientList,
                replyTo,
                subject,
                body,
                attachmentsPattern,
                attachBuildLog,
                contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {

        Run<?, ?> previousBuild = ExtendedEmailPublisher.getPreviousRun(build, listener);

        // ✅ cache current and previous test results
        AbstractTestResultAction<?> currentAction = build.getAction(AbstractTestResultAction.class);

        AbstractTestResultAction<?> previousAction =
                previousBuild != null ? previousBuild.getAction(AbstractTestResultAction.class) : null;

        // if no previous build
        if (previousBuild == null) {
            return build.getResult() == Result.FAILURE;
        }

        // if current build has no test results
        if (currentAction == null) {
            return false;
        }

        // if previous run didn't have test results
        if (previousAction == null) {
            return currentAction.getFailCount() > 0;
        }

        // if more tests failed in current build
        if (currentAction.getFailCount() > previousAction.getFailCount()) {
            return true;
        }

        // if any new test failed
        for (Object result : currentAction.getFailedTests()) {
            CaseResult res = (CaseResult) result;
            if (res.getAge() == 1) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldBypassThrottling() {
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            addTriggerNameToReplace(UnstableTrigger.TRIGGER_NAME);
            addTriggerNameToReplace(StillUnstableTrigger.TRIGGER_NAME);

            addDefaultRecipientProvider(new DevelopersRecipientProvider());
            addDefaultRecipientProvider(new ListRecipientProvider());
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }

        @Override
        public EmailTrigger createDefault() {
            return _createDefault();
        }
    }
}
