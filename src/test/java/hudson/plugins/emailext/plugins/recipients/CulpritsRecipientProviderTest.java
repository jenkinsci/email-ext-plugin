package hudson.plugins.emailext.plugins.recipients;

import hudson.model.Result;
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

public class CulpritsRecipientProviderTest {

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
    public void testAddRecipients1() throws Exception {
        final WorkflowJob j = Mockito.mock(WorkflowJob.class);
        final WorkflowRun build1 = Mockito.spy(new WorkflowRun(j));
        Mockito.when(build1.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build1, "X", "V");
        Mockito.doReturn(null).when(build1).getPreviousBuild();

        final WorkflowRun build2 = Mockito.spy(new WorkflowRun(j));
        Mockito.when(build2.getResult()).thenReturn(Result.SUCCESS);
        MockUtilities.addChangeSet(build2, "Z", "V");
        Mockito.doReturn(build1).when(build2).getPreviousCompletedBuild();

        final WorkflowRun build3 = Mockito.spy(new WorkflowRun(j));
        Mockito.when(build3.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build3, "A");
        Mockito.doReturn(build2).when(build3).getPreviousCompletedBuild();

        final WorkflowRun build4 = Mockito.spy(new WorkflowRun(j));
        Mockito.when(build4.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build4, "B");
        Mockito.doReturn(build3).when(build4).getPreviousCompletedBuild();

        TestUtilities.checkRecipients(build4, new CulpritsRecipientProvider(), "A", "B");
    }

    @Test
    public void testAddRecipients2() throws Exception {
        final WorkflowJob j = Mockito.mock(WorkflowJob.class);
        final WorkflowRun build1 = Mockito.spy(new WorkflowRun(j));
        Mockito.when(build1.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build1, "X", "V");
        Mockito.doReturn(null).when(build1).getPreviousBuild();

        final WorkflowRun build2 = Mockito.spy(new WorkflowRun(j));
        Mockito.when(build2.getResult()).thenReturn(Result.SUCCESS);
        MockUtilities.addChangeSet(build2, "Z", "V");
        Mockito.doReturn(build1).when(build2).getPreviousCompletedBuild();

        TestUtilities.checkRecipients(build2, new CulpritsRecipientProvider(), "X", "V", "Z");
    }
}
