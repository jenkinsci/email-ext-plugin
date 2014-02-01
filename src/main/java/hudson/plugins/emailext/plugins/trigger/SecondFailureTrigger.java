package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.plugins.emailext.plugins.RecipientProvider;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class SecondFailureTrigger extends NthFailureTrigger {

    public static final String TRIGGER_NAME = "Failure (Second)";

    @DataBoundConstructor
    public SecondFailureTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(2, recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Extension
    public static final class DescriptorImpl extends NthFailureTrigger.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }
    }
}
