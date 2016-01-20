package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.plugins.emailext.plugins.AbstractEmailTrigger;
import hudson.plugins.emailext.plugins.AbstractRecipientProvider;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class FirstFailureTrigger extends AbstractNthFailureTrigger {

    public static final String TRIGGER_NAME = "Failure - 1st";

    @DataBoundConstructor
    public FirstFailureTrigger(List<AbstractRecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(1, recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public FirstFailureTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(1, sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractDescriptorImpl {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }
        
        public AbstractEmailTrigger createDefault() {
            return _createDefault();
        }
    }

    /**
     * Maintaining backward compatibility
     *
     * @return this after checking for failureCount setting
     */
    public Object readResolve() {
        if (this.failureCount == 0) {
            this.failureCount = 1;
        }
        return this;
    }
}
