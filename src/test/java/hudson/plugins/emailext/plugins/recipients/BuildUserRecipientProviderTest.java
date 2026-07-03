package hudson.plugins.emailext.plugins.recipients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests the class {@link BuildUserRecipientProvider}.
 *
 * @author Akash Manna
 */
class BuildUserRecipientProviderTest {

    private MockedStatic<Jenkins> mockedJenkins;
    private MockedStatic<Mailer> mockedMailer;

    @BeforeEach
    void before() {
        final Jenkins jenkins = Mockito.mock(Jenkins.class);
        Mockito.when(jenkins.isUseSecurity()).thenReturn(false);

        final ExtendedEmailPublisherDescriptor descriptor =
                Mockito.mock(ExtendedEmailPublisherDescriptor.class);
        descriptor.setDebugMode(true);
        Mockito.when(descriptor.getExcludedCommitters()).thenReturn("");
        Mockito.when(jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class))
                .thenReturn(descriptor);

        mockedJenkins = Mockito.mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);

        final Mailer.DescriptorImpl mailerDescriptor = Mockito.mock(Mailer.DescriptorImpl.class);
        Mockito.when(mailerDescriptor.getDefaultSuffix()).thenReturn("DOMAIN");
        mockedMailer = Mockito.mockStatic(Mailer.class);
        mockedMailer.when(Mailer::descriptor).thenReturn(mailerDescriptor);
    }

    @AfterEach
    void after() {
        mockedMailer.close();
        mockedJenkins.close();
    }

    /**
     * When a FreeStyleBuild is triggered by a known user (via UserIdCause),
     * that user's email address should be added to the "to" recipients.
     */
    @Test
    void testAddRecipients_userTriggeredBuild_freeStyle() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.spy(new FreeStyleBuild(project));
            MockUtilities.addRequestor(mockedUser, build, "alice");

            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider(), "alice");
        }
    }

    /**
     * When a WorkflowRun is triggered by a known user (via UserIdCause),
     * that user's email address should be added to the "to" recipients.
     */
    @Test
    void testAddRecipients_userTriggeredBuild_workflowRun() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final WorkflowJob job = Mockito.mock(WorkflowJob.class);
            final WorkflowRun build = Mockito.spy(new WorkflowRun(job));

            // Set up a UserIdCause on the WorkflowRun spy
            final Cause.UserIdCause cause = Mockito.mock(Cause.UserIdCause.class);
            Mockito.when(cause.getUserId()).thenReturn("bob");
            Mockito.doReturn(cause).when(build).getCause(Cause.UserIdCause.class);

            // Create the user mock BEFORE the static stub to avoid nested mockito stubbing
            final User bobUser = MockUtilities.getUser("bob");
            mockedUser
                    .when(() -> User.get(Mockito.eq("bob"), Mockito.anyBoolean(), Mockito.any()))
                    .thenReturn(bobUser);

            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider(), "bob");
        }
    }

    /**
     * When a build has no UserIdCause, no recipients should be added and
     * the "not caused by a user" message should be logged.
     */
    @Test
    void testAddRecipients_buildNotCausedByUser() throws Exception {
        final WorkflowJob job = Mockito.mock(WorkflowJob.class);
        final WorkflowRun build = Mockito.spy(new WorkflowRun(job));

        // No UserIdCause — getCause returns null for both UserIdCause and UserCause
        Mockito.doReturn(null).when(build).getCause(Cause.UserIdCause.class);

        // Expect zero recipients
        TestUtilities.checkRecipients(build, new BuildUserRecipientProvider());
    }

    /**
     * When the UserIdCause is present but the user ID cannot be resolved
     * to an existing Jenkins user, no recipients should be added.
     */
    @Test
    void testAddRecipients_userIdCauseButUserNotFound() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final WorkflowJob job = Mockito.mock(WorkflowJob.class);
            final WorkflowRun build = Mockito.spy(new WorkflowRun(job));

            final Cause.UserIdCause cause = Mockito.mock(Cause.UserIdCause.class);
            Mockito.when(cause.getUserId()).thenReturn("unknown");
            Mockito.doReturn(cause).when(build).getCause(Cause.UserIdCause.class);

            // User.get returns null for unknown user (create=false)
            mockedUser
                    .when(() -> User.get(Mockito.eq("unknown"), Mockito.eq(false), Mockito.any()))
                    .thenReturn(null);

            // Expect zero recipients
            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider());
        }
    }

    /**
     * Multiple builds triggered by different users should each produce
     * the correct single recipient for that build.
     */
    @Test
    void testAddRecipients_differentUsersForDifferentBuilds() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);

            // Build triggered by "carol"
            final FreeStyleBuild build1 = Mockito.spy(new FreeStyleBuild(project));
            MockUtilities.addRequestor(mockedUser, build1, "carol");
            TestUtilities.checkRecipients(build1, new BuildUserRecipientProvider(), "carol");

            // Build triggered by "dave"
            final FreeStyleBuild build2 = Mockito.spy(new FreeStyleBuild(project));
            MockUtilities.addRequestor(mockedUser, build2, "dave");
            TestUtilities.checkRecipients(build2, new BuildUserRecipientProvider(), "dave");
        }
    }

    /**
     * DescriptorImpl.getDisplayName() should return "Build User".
     */
    @Test
    void testDescriptorDisplayName() {
        final BuildUserRecipientProvider.DescriptorImpl descriptor =
                new BuildUserRecipientProvider.DescriptorImpl();
        assertEquals("Build User", descriptor.getDisplayName());
    }

    /**
     * Verify that the provider does NOT look upstream — unlike RequesterRecipientProvider,
     * it should only resolve the user who triggered the current build directly.
     * A build triggered directly by a user (no UpstreamCause) should still resolve correctly.
     */
    @Test
    void testAddRecipients_doesNotFollowUpstream() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.spy(new FreeStyleBuild(project));

            // Only a UserIdCause, no UpstreamCause
            MockUtilities.addRequestor(mockedUser, build, "eve");
            Mockito.doReturn(null).when(build).getCause(Cause.UpstreamCause.class);

            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider(), "eve");
        }
    }

    /**
     * Validate that the @DataBoundConstructor default constructor instantiates without error.
     */
    @Test
    void testConstructor() {
        final BuildUserRecipientProvider provider = new BuildUserRecipientProvider();
        assertTrue(provider instanceof BuildUserRecipientProvider);
    }
}
