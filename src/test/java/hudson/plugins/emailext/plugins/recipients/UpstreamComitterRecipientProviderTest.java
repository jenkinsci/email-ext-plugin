package hudson.plugins.emailext.plugins.recipients;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import hudson.tasks.BuildTrigger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SequenceLock;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.mock_javamail.Mailbox;

public class UpstreamComitterRecipientProviderTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @After
    public void tearDown() {
        Mailbox.clearAll();
    }

    @Issue("JENKINS-46821")
    @Test
    public void multipleUpstreamJobs() throws Exception {
        FreeStyleProject us1 = j.createFreeStyleProject("us1");
        FakeChangeLogSCM scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("First Person <first@example.com>");
        us1.setScm(scm);
        us1.getPublishersList().add(new BuildTrigger("ds", Result.SUCCESS));

        FreeStyleProject us2 = j.createFreeStyleProject("us2");
        scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("Second Person <second@example.com>");
        us2.setScm(scm);
        us2.getPublishersList().add(new BuildTrigger("ds", Result.SUCCESS));

        FreeStyleProject ds = j.createFreeStyleProject("ds");
        ds.setQuietPeriod(0);
        SequenceLock seq = new SequenceLock();
        ds.getBuildersList()
                .add(
                        new TestBuilder() {
                            @Override
                            public boolean perform(
                                    AbstractBuild<?, ?> build,
                                    Launcher launcher,
                                    BuildListener listener)
                                    throws InterruptedException {
                                if (build.number == 1) {
                                    seq.phase(0);
                                    seq.phase(2);
                                }
                                return true;
                            }
                        });
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        SuccessTrigger successTrigger =
                new SuccessTrigger(
                        Collections.singletonList(new UpstreamComitterRecipientProvider()),
                        "",
                        "",
                        "",
                        "",
                        "",
                        0,
                        "project");
        publisher.getConfiguredTriggers().add(successTrigger);
        ds.getPublishersList().add(publisher);
        j.jenkins.rebuildDependencyGraph();

        ds.scheduleBuild2(0);
        seq.phase(1);
        Queue queue = j.jenkins.getQueue();
        assertTrue(queue.isEmpty());
        j.buildAndAssertSuccess(us1);
        j.buildAndAssertSuccess(us2);
        while (queue.isEmpty()) {
            Thread.sleep(100);
        }
        seq.done();
        j.waitUntilNoActivity();
        List<FreeStyleBuild> builds = new ArrayList<>(ds.getBuilds());
        assertEquals(2, builds.size());
        j.assertBuildStatusSuccess(builds.get(0));
        j.assertBuildStatusSuccess(builds.get(1));

        FreeStyleBuild build = ds.getLastBuild();
        j.assertLogContains("Email was triggered for: Success", build);
        assertEquals(1, Mailbox.get("first@example.com").size());
        assertEquals(1, Mailbox.get("second@example.com").size());
    }
}
