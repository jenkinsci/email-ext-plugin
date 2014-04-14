package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import java.util.List;

import hudson.tasks.junit.CaseResult;
import org.kohsuke.stapler.DataBoundConstructor;


public class RegressionTrigger extends EmailTrigger {
    
    public static final String TRIGGER_NAME = "Test Regression";
    
    @DataBoundConstructor
    public RegressionTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public RegressionTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        AbstractBuild<?,?> previousBuild = ExtendedEmailPublisher.getPreviousBuild(build, listener);
        if (previousBuild == null)
            return build.getResult() == Result.FAILURE;

        if (build.getTestResultAction() == null)
            return false;

        if (previousBuild.getTestResultAction() == null)
            return build.getTestResultAction().getFailCount() > 0;

        for (Object result : build.getTestResultAction().getFailedTests()){
            CaseResult res = (CaseResult)result;
            if (res.getAge() == 1)
                return true;
        }

        return build.getTestResultAction().getFailCount() > 
                previousBuild.getTestResultAction().getFailCount();
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

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
    }    
}
