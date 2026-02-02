package hudson.plugins.emailext.plugins.recipients;

import hudson.model.User;
import hudson.tasks.Mailer;
import java.util.Collections;
import jenkins.scm.api.metadata.ContributorMetadataAction;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.mock_javamail.Mailbox;

@WithJenkins
class ContributorMetadataRecipientProviderTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @AfterEach
    void tearDown() {
        Mailbox.clearAll();
    }

    @Test
    void testAddRecipients() throws Exception {
        User user = User.get("someone", true, Collections.emptyMap());
        user.addProperty(new Mailer.UserProperty("someone@DOMAIN"));

        WorkflowJob job = j.createProject(WorkflowJob.class, "test");
        WorkflowRun run = job.scheduleBuild2(0).get();
        run.addAction(new ContributorMetadataAction("someone", "Some One", "someone@DOMAIN"));

        TestUtilities.checkRecipients(run, new ContributorMetadataRecipientProvider(), "someone");
    }

    @Test
    void testAddRecipients_NotUser() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "test");
        WorkflowRun run = job.scheduleBuild2(0).get();
        run.addAction(new ContributorMetadataAction("someoneelse", "Some One Else", "someoneelse@DOMAIN"));

        TestUtilities.checkRecipients(run, new ContributorMetadataRecipientProvider());
    }
}
