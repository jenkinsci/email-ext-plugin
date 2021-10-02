package hudson.plugins.emailext.plugins.recipients;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DevelopersRecipientProviderTest {

    private MockedStatic<Jenkins> mockedJenkins;
    private MockedStatic<Mailer> mockedMailer;

    @Before
    public void before() {
        final Jenkins jenkins = Mockito.mock(Jenkins.class);
        Mockito.when(jenkins.isUseSecurity()).thenReturn(false);
        final ExtendedEmailPublisherDescriptor extendedEmailPublisherDescriptor = Mockito.mock(ExtendedEmailPublisherDescriptor.class);
        extendedEmailPublisherDescriptor.setDebugMode(true);
        Mockito.when(extendedEmailPublisherDescriptor.getExcludedCommitters()).thenReturn("");

        Mockito.when(jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class)).thenReturn(extendedEmailPublisherDescriptor);
        mockedJenkins = Mockito.mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);

        final Mailer.DescriptorImpl descriptor = Mockito.mock(Mailer.DescriptorImpl.class);
        Mockito.when(descriptor.getDefaultSuffix()).thenReturn("DOMAIN");
        mockedMailer = Mockito.mockStatic(Mailer.class);
        mockedMailer.when(Mailer::descriptor).thenReturn(descriptor);
    }

    @After
    public void after() {
        mockedMailer.close();
        mockedJenkins.close();
    }

    @Test
    public void testAddRecipients() throws Exception {
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject p = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build1 = Mockito.spy(new FreeStyleBuild(p));
            Mockito.doReturn(Result.UNSTABLE).when(build1).getResult();
            MockUtilities.addRequestor(mockedUser, build1, "A");
            MockUtilities.addChangeSet(build1, "X", "V");
            TestUtilities.checkRecipients(build1, new DevelopersRecipientProvider(), "X", "V");
        }

        final WorkflowJob j = Mockito.mock(WorkflowJob.class);
        final WorkflowRun build2 = Mockito.spy(new WorkflowRun(j));
        Mockito.doReturn(Result.UNSTABLE).when(build2).getResult();
        MockUtilities.addChangeSet(build2, "X", "V");
        TestUtilities.checkRecipients(build2, new DevelopersRecipientProvider(), "X", "V");
    }
}
