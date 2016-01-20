package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import jenkins.model.Jenkins;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.AbstractEmailTrigger;
import hudson.plugins.emailext.plugins.AbstractEmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.AbstractRecipientProvider;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;


public class FailureTrigger extends AbstractEmailTrigger {

    public static final String TRIGGER_NAME = "Failure - Any";
    
    @Deprecated
    public static FailureTrigger createDefault() {
        DescriptorImpl descriptor = (DescriptorImpl) Jenkins.getActiveInstance().getDescriptor(FailureTrigger.class);
        return (FailureTrigger) descriptor.createDefault();
    }
    
    @DataBoundConstructor
    public FailureTrigger(List<AbstractRecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body,
                          String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public FailureTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, 
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        return build.getResult() == Result.FAILURE;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractEmailTriggerDescriptor {

        public DescriptorImpl() {
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
