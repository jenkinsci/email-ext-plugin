package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class WorkflowTrigger extends EmailTrigger {

    private final String triggerName;

    @DataBoundConstructor
    public WorkflowTrigger(String triggerName, String recipientList, String replyTo, String subject, String body, String attachmentsPattern,
                           int attachBuildLog, String contentType) {
        super(null, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
        this.triggerName = (triggerName != null ? triggerName : "Workflow");
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
                return triggerName;
            }
        };
    }
}
