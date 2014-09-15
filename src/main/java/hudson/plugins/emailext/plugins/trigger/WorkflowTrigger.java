package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class WorkflowTrigger extends EmailTrigger {

    @DataBoundConstructor
    public WorkflowTrigger(String recipientList, String replyTo, String subject, String body, String attachmentsPattern,
                           int attachBuildLog, String contentType) {
        super(null, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        throw new IllegalStateException("Unexpected call to WorkflowTrigger.trigger().");
    }

    @Override
    public EmailTriggerDescriptor getDescriptor() {
        return new EmailTriggerDescriptor() {

            @Override
            public String getDisplayName() {
                return "Workflow";
            }
        };
    }
}
