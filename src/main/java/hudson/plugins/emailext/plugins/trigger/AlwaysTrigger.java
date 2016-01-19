package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;

import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author acearl
 */
public class AlwaysTrigger extends hudson.plugins.emailext.plugins.AbstractEmailTrigger {

    public static final String TRIGGER_NAME = "Always";
    
    @DataBoundConstructor
    public AlwaysTrigger(List<hudson.plugins.emailext.plugins.AbstractRecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public AlwaysTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends hudson.plugins.emailext.plugins.AbstractEmailTriggerDescriptor {
        
        public DescriptorImpl() {
            addDefaultRecipientProvider(new DevelopersRecipientProvider());
            addDefaultRecipientProvider(new ListRecipientProvider());
        }

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }
        
        @Override
        public hudson.plugins.emailext.plugins.AbstractEmailTrigger createDefault() {
            return _createDefault();
        }
    }   
}

