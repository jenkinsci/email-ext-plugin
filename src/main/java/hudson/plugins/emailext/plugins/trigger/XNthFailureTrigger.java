package hudson.plugins.emailext.plugins.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Kanstantsin Shautsou
 */
public class XNthFailureTrigger extends NthFailureTrigger {
    public static final String TRIGGER_NAME = "Failure - X";

    private int requiredFailureCount = 3;

    @DataBoundConstructor
    public XNthFailureTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo,
                              String subject, String body, String attachmentsPattern, int attachBuildLog,
                              String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public int getRequiredFailureCount() {
        return requiredFailureCount;
    }

    @DataBoundSetter
    public void setRequiredFailureCount(int requiredFailureCount) {
        this.requiredFailureCount = requiredFailureCount;
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
