package hudson.plugins.emailext.plugins.recipients;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class RequesterRecipientProviderTest {
    private MockedStatic<Jenkins> mockedJenkins;
    private MockedStatic<Mailer> mockedMailer;
    private Jenkins jenkins;

    @BeforeEach
    void before() {
        jenkins = Mockito.mock(Jenkins.class);
        Mockito.when(jenkins.isUseSecurity()).thenReturn(false);

        final ExtendedEmailPublisherDescriptor descriptor = Mockito.mock(ExtendedEmailPublisherDescriptor.class);
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

    @Test
    @DisplayName("User who directly triggered the build receives email when no upstream cause exists")
    void testDirectUserTriggerReceivesEmail() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.mock(FreeStyleBuild.class);

            Mockito.doReturn(Result.FAILURE).when(build).getResult();
            Mockito.doReturn(null).when(build).getCause(Cause.UpstreamCause.class);

            MockUtilities.addRequestor(mockedUser, build, "A");
            TestUtilities.checkRecipients(build, new RequesterRecipientProvider(), "A");
        }
    }

    @Test
    @DisplayName("No email sent when build was not triggered by a user and has no upstream cause")
    void testNoEmailWhenNoUserAndNoUpstreamCause() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.mock(FreeStyleBuild.class);

            Mockito.doReturn(Result.FAILURE).when(build).getResult();
            Mockito.doReturn(null).when(build).getCause(Cause.UpstreamCause.class);
            Mockito.doReturn(null).when(build).getCause(Cause.UserIdCause.class);

            TestUtilities.checkRecipients(build, new RequesterRecipientProvider());
        }
    }

    @Test
    @DisplayName("Original upstream user receives email when build was triggered by upstream project")
    void testUpstreamUserReceivesEmail() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject upstreamProject = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild upstreamBuild = Mockito.mock(FreeStyleBuild.class);

            Mockito.doReturn(Result.SUCCESS).when(upstreamBuild).getResult();
            Mockito.doReturn(null).when(upstreamBuild).getCause(Cause.UpstreamCause.class);
            MockUtilities.addRequestor(mockedUser, upstreamBuild, "A");

            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.mock(FreeStyleBuild.class);

            Mockito.doReturn(Result.FAILURE).when(build).getResult();

            Cause.UpstreamCause upstreamCause = Mockito.mock(Cause.UpstreamCause.class);
            Mockito.when(upstreamCause.getUpstreamProject()).thenReturn("upstream-project");
            Mockito.when(upstreamCause.getUpstreamBuild()).thenReturn(1);
            Mockito.doReturn(upstreamCause).when(build).getCause(Cause.UpstreamCause.class);

            Mockito.when(jenkins.getItemByFullName("upstream-project")).thenReturn((Job) upstreamProject);
            Mockito.when(upstreamProject.getBuildByNumber(1)).thenReturn((FreeStyleBuild) upstreamBuild);

            TestUtilities.checkRecipients(build, new RequesterRecipientProvider(), "A");
        }
    }

    @Test
    @DisplayName("No email and no crash when upstream project linkage is broken")
    void testBrokenUpstreamProjectLinkage() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.mock(FreeStyleBuild.class);

            Mockito.doReturn(Result.FAILURE).when(build).getResult();

            Cause.UpstreamCause upstreamCause = Mockito.mock(Cause.UpstreamCause.class);
            Mockito.when(upstreamCause.getUpstreamProject()).thenReturn("missing-project");
            Mockito.doReturn(upstreamCause).when(build).getCause(Cause.UpstreamCause.class);

            Mockito.when(jenkins.getItemByFullName("missing-project")).thenReturn(null);

            TestUtilities.checkRecipients(build, new RequesterRecipientProvider());
        }
    }

    @Test
    @DisplayName("No email and no crash when upstream project exists but build is not found")
    void testUpstreamProjectExistsButBuildIsNotFound() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.mock(FreeStyleBuild.class);

            Mockito.doReturn(Result.FAILURE).when(build).getResult();

            Cause.UpstreamCause upstreamCause = Mockito.mock(Cause.UpstreamCause.class);
            Mockito.when(upstreamCause.getUpstreamProject()).thenReturn("existing-project");
            Mockito.when(upstreamCause.getUpstreamBuild()).thenReturn(1);
            Mockito.doReturn(upstreamCause).when(build).getCause(Cause.UpstreamCause.class);

            final FreeStyleProject upstreamProject = Mockito.mock(FreeStyleProject.class);
            Mockito.when(jenkins.getItemByFullName("existing-project")).thenReturn((Job) upstreamProject);

            Mockito.when(upstreamProject.getBuildByNumber(1)).thenReturn(null);

            TestUtilities.checkRecipients(build, new RequesterRecipientProvider());
        }
    }
}
