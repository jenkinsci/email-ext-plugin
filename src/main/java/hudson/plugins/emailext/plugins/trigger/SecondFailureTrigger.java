package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class SecondFailureTrigger extends NthFailureTrigger {

    public static final String TRIGGER_NAME = "2nd Failure";

    @DataBoundConstructor
    public SecondFailureTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, boolean sendToUpstream, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(2, sendToList, sendToDevs, sendToRequestor, sendToCulprits, sendToUpstream, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Extension
    public static final class DescriptorImpl extends NthFailureTrigger.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }
    }
}
