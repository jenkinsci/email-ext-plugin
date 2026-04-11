package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.plugins.recipients.RequesterRecipientProvider;
import hudson.tasks.Mailer;
import java.util.Set;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests the class {@link EmailExtRecipientStep}.
 *
 * @author Akash Manna
 */
@WithJenkins
class EmailExtRecipientStepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @Test
    void descriptorMetadata() {
        EmailExtRecipientStep.DescriptorImpl descriptor = new EmailExtRecipientStep.DescriptorImpl();

        assertEquals("emailextrecipients", descriptor.getFunctionName());
        Set<? extends Class<?>> requiredContext = descriptor.getRequiredContext();
        assertTrue(requiredContext.contains(Run.class));
        assertTrue(requiredContext.contains(TaskListener.class));
        assertTrue(requiredContext.contains(EnvVars.class));

        assertTrue(descriptor.getRecipientProvidersDescriptors().stream()
                .anyMatch(d -> d instanceof RequesterRecipientProvider.DescriptorImpl));
    }

    @Test
    void failsWhenNoRecipientProviders() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "wf-empty-providers");
        job.setDefinition(new CpsFlowDefinition("node { emailextrecipients(recipientProviders: []) }", true));

        Run<?, ?> run = job.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, run);
        j.assertLogContains("You must provide at least one recipient provider", run);
    }

    @Test
    void aggregatesRecipientsFromProviders() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "wf-recipients");

        User user = User.getById("kutzi", true);
        user.setFullName("Christoph Kutzinski");
        user.addProperty(new Mailer.UserProperty("kutzi@xxx.com"));

        job.setDefinition(new CpsFlowDefinition(
                "node { "
                        + "def recipients = emailextrecipients([requestor()]); "
                        + "echo \"RECIPS=${recipients}\""
                        + " }",
                true));

        Run<?, ?> run = job.scheduleBuild2(0, new CauseAction(new Cause.UserIdCause("kutzi")))
                .get();
        j.assertBuildStatusSuccess(run);

        String log = String.join("\n", run.getLog(2000));
        assertTrue(log.contains("RECIPS="), "Pipeline should print resolved recipients");
        assertTrue(log.contains("kutzi@xxx.com"), "Resolved recipients should include the triggering user");
    }
}
