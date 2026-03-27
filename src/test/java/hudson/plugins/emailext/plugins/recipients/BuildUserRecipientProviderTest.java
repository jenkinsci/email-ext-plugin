package hudson.plugins.emailext.plugins.recipients;

import hudson.model.Cause.UserIdCause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
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

class BuildUserRecipientProviderTest {

    private MockedStatic<Jenkins> mockedJenkins;
    private MockedStatic<Mailer> mockedMailer;

    @BeforeEach
    void before() {
        final Jenkins jenkins = Mockito.mock(Jenkins.class);
        Mockito.when(jenkins.isUseSecurity()).thenReturn(false);
        final ExtendedEmailPublisherDescriptor descriptor = Mockito.mock(ExtendedEmailPublisherDescriptor.class);
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

    @Test
    @DisplayName("User who triggered build should receive email")
    void testUserWhoTriggeredBuildReceivesEmail() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.spy(new FreeStyleBuild(project));
            Mockito.doReturn(Result.FAILURE).when(build).getResult();
            MockUtilities.addRequestor(mockedUser, build, "A");
            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider(), "A");
        }
    }

    @Test
    @DisplayName("No email sent when build was not triggered by a user")
    void testNoEmailWhenBuildNotTriggeredByUser() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.spy(new FreeStyleBuild(project));
            Mockito.doReturn(Result.FAILURE).when(build).getResult();

            // Prevent deep Jenkins core calls in lightweight unit tests
            Mockito.doReturn(null).when(build).getCause(UserIdCause.class);

            // No addRequestor call - simulates SCM/timer-triggered build
            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider());
        }
    }

    @Test
    @DisplayName("Only triggering user receives email, not changeset committers")
    void testOnlyTriggeringUserReceivesEmailNotCommitters() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.spy(new FreeStyleBuild(project));
            Mockito.doReturn(Result.FAILURE).when(build).getResult();
            MockUtilities.addRequestor(mockedUser, build, "A");
            MockUtilities.addChangeSet(build, "X", "Y");
            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider(), "A");
        }
    }

    @Test
    @DisplayName("Triggering user receives email regardless of build result")
    void testEmailSentRegardlessOfBuildResult() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.spy(new FreeStyleBuild(project));
            Mockito.doReturn(Result.SUCCESS).when(build).getResult();
            MockUtilities.addRequestor(mockedUser, build, "A");
            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider(), "A");
        }
    }
}
