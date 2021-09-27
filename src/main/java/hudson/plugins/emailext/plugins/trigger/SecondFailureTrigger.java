package hudson.plugins.emailext.plugins.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class SecondFailureTrigger extends NthFailureTrigger {

    public static final String TRIGGER_NAME = "Failure - 2nd";

    @DataBoundConstructor
    public SecondFailureTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public SecondFailureTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(2, sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    protected int getRequiredFailureCount() {
        return 2;
    }

    @Extension
    public static final class DescriptorImpl extends NthFailureTrigger.DescriptorImpl {

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
