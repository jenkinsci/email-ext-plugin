package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class StatusChangedTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Status Changed";

    @DataBoundConstructor
    public StatusChangedTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        final Result buildResult = build.getResult();

        if (buildResult != null) {
            final AbstractBuild<?, ?> prevBuild = ExtendedEmailPublisher.getPreviousBuild(build, listener);

            if (prevBuild == null) {
            	// Notify at the first status defined
            	return true;
            }

            return (build.getResult() != prevBuild.getResult());
        }

        return false;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {
        
        public DescriptorImpl() {
            addDefaultRecipientProvider(new DevelopersRecipientProvider());
        }
        
        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }
    }
}
