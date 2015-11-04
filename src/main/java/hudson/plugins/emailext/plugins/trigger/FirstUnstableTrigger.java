package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 */
public class FirstUnstableTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Unstable (Test Failures) - 1st";

    @DataBoundConstructor
    public FirstUnstableTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body,
            String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Deprecated
    public FirstUnstableTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        Run<?, ?> previousRun = ExtendedEmailPublisher.getPreviousRun(build, listener);
        return previousRun != null
                ? previousRun.getResult() != Result.UNSTABLE && build.getResult() == Result.UNSTABLE
                : build.getResult() == Result.UNSTABLE;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            addTriggerNameToReplace(UnstableTrigger.TRIGGER_NAME);
            addTriggerNameToReplace(StatusChangedTrigger.TRIGGER_NAME);

            addDefaultRecipientProvider(new DevelopersRecipientProvider());
            addDefaultRecipientProvider(new ListRecipientProvider());
        }

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
