package hudson.plugins.emailext.plugins.recipients;

import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import jenkins.scm.api.metadata.ContributorMetadataAction;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.mock_javamail.Mailbox;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;

public class ContributorMetadataRecipientProviderTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @After
    public void tearDown() {
        Mailbox.clearAll();
    }

    @Test
    public void testAddRecipients() throws Exception {
        User user = User.get("someone", true, Collections.emptyMap());
        user.addProperty(new Mailer.UserProperty("someone@DOMAIN"));

        WorkflowJob job = j.createProject(WorkflowJob.class, "test");
        WorkflowRun run = job.scheduleBuild2(0).get();
        run.addAction(new ContributorMetadataAction("someone", "Some One", "someone@DOMAIN" ));

        TestUtilities.checkRecipients(run, new ContributorMetadataRecipientProvider(), "someone");
    }

    @Test
    public void testAddRecipients_NotUser() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class, "test");
        WorkflowRun run = job.scheduleBuild2(0).get();
        run.addAction(new ContributorMetadataAction("someoneelse", "Some One Else", "someoneelse@DOMAIN" ));

        TestUtilities.checkRecipients(run, new ContributorMetadataRecipientProvider());
    }
}
