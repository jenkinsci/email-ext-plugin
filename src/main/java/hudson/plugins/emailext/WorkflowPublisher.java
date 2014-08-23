package hudson.plugins.emailext;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.trigger.WorkflowTrigger;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class WorkflowPublisher extends ExtendedEmailPublisher implements SimpleBuildStep {

    private final String triggerName;

    @DataBoundConstructor
    public WorkflowPublisher(String triggerName, String recipientList, String replyto, String subject,
                             String content, String contentType, String attachments, boolean attachBuildlog,
                             boolean compressBuildlog, String presendScript, boolean saveOutput) {
        super(recipientList, contentType, subject, content, attachments, presendScript,
                toAttachBuildlogCode(attachBuildlog, compressBuildlog), replyto,
                saveOutput, null, null, false, null);
        this.triggerName = triggerName;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        ExtendedEmailPublisherContext context = new ExtendedEmailPublisherContext(this, workspace, run, launcher, listener);
        WorkflowTrigger workflowTrigger =
                new WorkflowTrigger(
                        triggerName,
                        recipientList != null ? recipientList : DEFAULT_RECIPIENTS_TEXT,
                        replyTo != null ? replyTo : "",
                        defaultSubject != null ? defaultSubject : PROJECT_DEFAULT_SUBJECT_TEXT,
                        defaultContent != null ? defaultContent : PROJECT_DEFAULT_BODY_TEXT,
                        attachmentsPattern != null ? attachmentsPattern : "",
                        project_attach_buildlog,
                        contentType != null ? contentType : "project");
        final Multimap<String, EmailTrigger> triggered = ArrayListMultimap.create();

        triggered.put("Workflow", workflowTrigger);
        context.setTriggered(triggered);
        context.setTrigger(workflowTrigger);

        sendMail(context);
    }

    @Override
    public ExtendedEmailPublisherDescriptor getDescriptor() {
        return (ExtendedEmailPublisherDescriptor) Jenkins.getInstance().getDescriptor(ExtendedEmailPublisher.class);
    }

    private static int toAttachBuildlogCode(boolean attachBuildlog, boolean compressBuildlog) {
        if (attachBuildlog && !compressBuildlog) {
            return 1;
        } else if (attachBuildlog && compressBuildlog) {
            return 2;
        }
        return 0;
    }
}
