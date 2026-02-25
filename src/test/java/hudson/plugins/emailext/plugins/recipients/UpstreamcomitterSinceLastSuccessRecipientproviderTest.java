package hudson.plugins.emailext.plugins.recipients;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.trigger.AlwaysTrigger;
import hudson.plugins.emailext.plugins.trigger.FailureTrigger;
import hudson.tasks.BuildTrigger;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.mock_javamail.Mailbox;

@WithJenkins
class UpstreamComitterSinceLastSuccessRecipientProviderTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @AfterEach
    void tearDown() {
        Mailbox.clearAll();
    }

    /**
     * Covers: early-return path (lastSuccessfulBuild == null).
     * Job B has never succeeded. Provider should return early and send no email
     * to anyone. This tests the null-guard at the top of addRecipients().
     */
    @Test
    void noEmailWhenJobBNeverSucceeded() throws Exception {
        FreeStyleProject jobA = j.createFreeStyleProject("jobA");
        FakeChangeLogSCM scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("First Person <first@example.com>");
        jobA.setScm(scm);
        jobA.getPublishersList().add(new BuildTrigger("jobB", Result.SUCCESS));

        FreeStyleProject jobB = j.createFreeStyleProject("jobB");
        jobB.setQuietPeriod(0);
        jobB.getBuildersList().add(new hudson.tasks.Shell("exit 1"));

        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher
                .getConfiguredTriggers()
                .add(new FailureTrigger(
                        Collections.singletonList(new UpstreamComitterSinceLastSuccessRecipientProvider()),
                        "",
                        "",
                        "",
                        "",
                        "",
                        0,
                        "project"));
        jobB.getPublishersList().add(publisher);
        j.jenkins.rebuildDependencyGraph();

        j.buildAndAssertSuccess(jobA);
        j.waitUntilNoActivity();

        assertEquals(0, Mailbox.get("first@example.com").size());
    }

    /**
     * Covers: happy path — single committer in window.
     *
     * Exercises the full addRecipients() main path:
     *   - lastSuccessfulBuild found (not null)
     *   - while loop walks back one Job B build
     *   - collectUpstreamBuilds() finds Job A build
     *   - addUpstreamCommittersTriggeringBuild() iterates the change set
     *   - addUserFromChangeSet() resolves and adds Second Person
     *
     * Job A #1 (First Person)   -> Job B #1 SUCCESS  (anchor established)
     * Job A #2 (Second Person) -> Job B #2 FAILS    (current)
     * Expected: only Second Person notified, First Person is behind the anchor.
     */
    @Test
    void onlyCommitterSinceLastSuccessIsNotified() throws Exception {
        FreeStyleProject jobA = j.createFreeStyleProject("jobA");
        jobA.getPublishersList().add(new BuildTrigger("jobB", Result.SUCCESS));

        FreeStyleProject jobB = j.createFreeStyleProject("jobB");
        jobB.setQuietPeriod(0);

        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher
                .getConfiguredTriggers()
                .add(new AlwaysTrigger(
                        Collections.singletonList(new UpstreamComitterSinceLastSuccessRecipientProvider()),
                        "",
                        "",
                        "",
                        "",
                        "",
                        0,
                        "project"));
        jobB.getPublishersList().add(publisher);
        j.jenkins.rebuildDependencyGraph();

        // Build #1: First Person -> Job B SUCCESS (sets the anchor)
        FakeChangeLogSCM scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("First Person <first@example.com>");
        jobA.setScm(scm);
        j.buildAndAssertSuccess(jobA);
        j.waitUntilNoActivity();
        Mailbox.clearAll(); // reset — we only care about what happens after the anchor

        // Build #2: Mahrous -> Job B FAILS
        scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("Second Person <second@example.com>");
        jobA.setScm(scm);
        jobB.getBuildersList().add(new hudson.tasks.Shell("exit 1"));
        j.buildAndAssertSuccess(jobA);
        j.waitUntilNoActivity();

        assertEquals(1, Mailbox.get("second@example.com").size(), "Second Person should be notified");
        assertEquals(0, Mailbox.get("first@example.com").size(), "First Person is behind the anchor");
    }

    /**
     * Covers: while-loop walking back multiple Job B builds + accumulation.
     * Job A #1 (First Person)   -> Job B #1 SUCCESS  (anchor)
     * Job A #2 (Second Person) -> Job B #2 FAILS
     * Job A #3 (Third Person)   -> Job B #3 FAILS    (current build)
     * Expected: Second Person AND Third Person notified. First Person is behind the anchor.
     */
    @Test
    void multipleFailuresAccumulateCommitters() throws Exception {
        FreeStyleProject jobA = j.createFreeStyleProject("jobA");
        jobA.getPublishersList().add(new BuildTrigger("jobB", Result.SUCCESS));

        FreeStyleProject jobB = j.createFreeStyleProject("jobB");
        jobB.setQuietPeriod(0);

        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher
                .getConfiguredTriggers()
                .add(new AlwaysTrigger(
                        Collections.singletonList(new UpstreamComitterSinceLastSuccessRecipientProvider()),
                        "",
                        "",
                        "",
                        "",
                        "",
                        0,
                        "project"));
        jobB.getPublishersList().add(publisher);
        j.jenkins.rebuildDependencyGraph();

        // Build #1: First Person -> Job B SUCCESS (anchor)
        FakeChangeLogSCM scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("First Person <first@example.com>");
        jobA.setScm(scm);
        j.buildAndAssertSuccess(jobA);
        j.waitUntilNoActivity();
        Mailbox.clearAll();

        // Build #2: Second Person -> Job B FAILS
        scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("Second Person <second@example.com>");
        jobA.setScm(scm);
        jobB.getBuildersList().add(new hudson.tasks.Shell("exit 1"));
        j.buildAndAssertSuccess(jobA);
        j.waitUntilNoActivity();
        Mailbox.clearAll();

        // Build #3: Third Person -> Job B FAILS (anchor still at #1)
        scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("Third Person <third@example.com>");
        jobA.setScm(scm);
        j.buildAndAssertSuccess(jobA);
        j.waitUntilNoActivity();

        assertEquals(1, Mailbox.get("second@example.com").size(), "Second Person should be notified — still in window");
        assertEquals(
                1,
                Mailbox.get("third@example.com").size(),
                "Third Person should be notified — triggered current build");
        assertEquals(0, Mailbox.get("first@example.com").size(), "First Person is behind the anchor");
    }

    /**
     * Covers: anchor advances after recovery AND recursive collectUpstreamBuilds()
     * via a multi-level chain (Job A -> Job B -> Job C, email on Job C).
     *
     * Two Checks in one test:
     *   1. After Job C succeeds, the anchor moves forward — old committers drop out.
     *   2. collectUpstreamBuilds() recursively walks Job C -> Job B -> Job A
     *      to find the actual SCM commits (Job B and Job C have no SCM themselves).
     *
     * Job A #1 (First Person)   -> Job B #1 -> Job C #1 SUCCESS (anchor)
     * Job A #2 (Second Person) -> Job B #2 -> Job C #2 SUCCESS (anchor advances)
     * Job A #3 (Third Person)   -> Job B #3 -> Job C #3 FAILS
     * Expected: only Third Person notified. First Person and Second Pwerson are behind the new anchor.
     */
    @Test
    void anchorAdvancesAndMultiLevelTreeIsTraversed() throws Exception {
        FreeStyleProject jobA = j.createFreeStyleProject("jobA");
        jobA.getPublishersList().add(new BuildTrigger("jobB", Result.SUCCESS));

        FreeStyleProject jobB = j.createFreeStyleProject("jobB");
        jobB.setQuietPeriod(0);
        jobB.getPublishersList().add(new BuildTrigger("jobC", Result.SUCCESS));

        FreeStyleProject jobC = j.createFreeStyleProject("jobC");
        jobC.setQuietPeriod(0);

        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher
                .getConfiguredTriggers()
                .add(new AlwaysTrigger(
                        Collections.singletonList(new UpstreamComitterSinceLastSuccessRecipientProvider()),
                        "",
                        "",
                        "",
                        "",
                        "",
                        0,
                        "project"));
        jobC.getPublishersList().add(publisher);
        j.jenkins.rebuildDependencyGraph();

        // Build #1: First Person -> full chain SUCCESS (anchor for Job C)
        FakeChangeLogSCM scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("First Person <first@example.com>");
        jobA.setScm(scm);
        j.buildAndAssertSuccess(jobA);
        j.waitUntilNoActivity();
        Mailbox.clearAll();

        // Build #2: Second Person -> full chain SUCCESS (anchor advances)
        scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("Second Person <second@example.com>");
        jobA.setScm(scm);
        j.buildAndAssertSuccess(jobA);
        j.waitUntilNoActivity();
        Mailbox.clearAll();

        // Build #3: Third Person -> Job C FAILS
        scm = new FakeChangeLogSCM();
        scm.addChange().withAuthor("Third Person <third@example.com>");
        jobA.setScm(scm);
        jobC.getBuildersList().add(new hudson.tasks.Shell("exit 1"));
        j.buildAndAssertSuccess(jobA);
        j.waitUntilNoActivity();

        assertEquals(1, Mailbox.get("third@example.com").size(), "Third Person should be notified");
        assertEquals(0, Mailbox.get("second@example.com").size(), "Second Person is behind the new anchor");
        assertEquals(0, Mailbox.get("first@example.com").size(), "First Person is behind the new anchor");
    }
}
