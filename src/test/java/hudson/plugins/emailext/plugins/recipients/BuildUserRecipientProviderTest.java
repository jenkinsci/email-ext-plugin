package hudson.plugins.emailext.plugins.recipients;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void testUserWhoTriggeredBuildReceivesEmail() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.spy(new FreeStyleBuild(project));
            Mockito.doReturn(Result.FAILURE).when(build).getResult();
            // User "A" triggered the build via UserIdCause
            MockUtilities.addRequestor(mockedUser, build, "A");
            // User "A" should receive the email
            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider(), "A");
        }
    }

    @Test
    void testNoEmailWhenBuildNotTriggeredByUser() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.spy(new FreeStyleBuild(project));
            Mockito.doReturn(Result.FAILURE).when(build).getResult();
            // No UserIdCause — simulates a timer/SCM-triggered build
            Mockito.doReturn(null).when(build).getCause(hudson.model.Cause.UserIdCause.class);
            // No recipients expected
            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider());
        }
    }

    @Test
    void testOnlyTriggeringUserReceivesEmailNotCommitters() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build = Mockito.spy(new FreeStyleBuild(project));
            Mockito.doReturn(Result.FAILURE).when(build).getResult();
            // User "A" triggered the build, users "X" and "Y" made commits
            MockUtilities.addRequestor(mockedUser, build, "A");
            MockUtilities.addChangeSet(build, "X", "Y");
            // Only "A" should receive email — not the committers X and Y
            TestUtilities.checkRecipients(build, new BuildUserRecipientProvider(), "A");
        }
    }
}
