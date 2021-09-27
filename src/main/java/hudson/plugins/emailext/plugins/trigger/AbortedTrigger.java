package hudson.plugins.emailext.plugins.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class AbortedTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Aborted";
    
    @DataBoundConstructor
    public AbortedTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Deprecated
    public AbortedTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        return build.getResult() == Result.ABORTED;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {
        
        public DescriptorImpl() {
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
