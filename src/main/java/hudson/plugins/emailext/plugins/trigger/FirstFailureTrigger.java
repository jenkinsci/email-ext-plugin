package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

public class FirstFailureTrigger extends NthFailureTrigger {

    public static final String TRIGGER_NAME = "1st Failure";

    @DataBoundConstructor
    public FirstFailureTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(1, sendToList, sendToDevs, sendToRequestor, sendToCulprits, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Extension
    public static final class DescriptorImpl extends NthFailureTrigger.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
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
