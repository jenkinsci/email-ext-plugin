package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.AbstractEmailTrigger;
import hudson.plugins.emailext.plugins.AbstractEmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.AbstractRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;


public class StillUnstableTrigger extends AbstractEmailTrigger {

    public static final String TRIGGER_NAME = "Unstable (Test Failures) - Still";
    
    @DataBoundConstructor
    public StillUnstableTrigger(List<AbstractRecipientProvider> recipientProviders, String recipientList, String replyTo,
                                String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public StillUnstableTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        Result buildResult = build.getResult();

        if (buildResult == Result.UNSTABLE) {
            Run<?,?> prevRun = ExtendedEmailPublisher.getPreviousRun(build, listener);
            if (prevRun != null && (prevRun.getResult() == Result.UNSTABLE)) {
                return true;
            }
        }

        return false;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractEmailTriggerDescriptor {

        public DescriptorImpl() {
            addTriggerNameToReplace(UnstableTrigger.TRIGGER_NAME);
            
            addDefaultRecipientProvider(new DevelopersRecipientProvider());
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
