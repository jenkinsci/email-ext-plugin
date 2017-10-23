package hudson.plugins.emailext.plugins.recipients;

import hudson.model.FreeStyleBuild;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ExtendedEmailPublisherDescriptor.class,
        FreeStyleBuild.class,
        Jenkins.class,
        Mailer.class,
        Mailer.DescriptorImpl.class,
        User.class,
        WorkflowRun.class,
        WorkflowJob.class,
        Job.class
})
public class CulpritsRecipientProviderTest {

    @Before
    public void before() throws Exception {
        final Jenkins jenkins = PowerMockito.mock(Jenkins.class);
        PowerMockito.when(jenkins.isUseSecurity()).thenReturn(false);
        final ExtendedEmailPublisherDescriptor extendedEmailPublisherDescriptor = PowerMockito.mock(ExtendedEmailPublisherDescriptor.class);
        extendedEmailPublisherDescriptor.setDebugMode(true);
        PowerMockito.when(extendedEmailPublisherDescriptor.getExcludedCommitters()).thenReturn("");

        PowerMockito.when(jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class)).thenReturn(extendedEmailPublisherDescriptor);
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.doReturn(jenkins).when(Jenkins.class, "getActiveInstance");

        final Mailer.DescriptorImpl descriptor = PowerMockito.mock(Mailer.DescriptorImpl.class);
        PowerMockito.when(descriptor.getDefaultSuffix()).thenReturn("DOMAIN");
        PowerMockito.mockStatic(Mailer.class);
        PowerMockito.doReturn(descriptor).when(Mailer.class, "descriptor");
    }

    @Test
    public void testAddRecipients1() throws Exception {
        final WorkflowJob j = PowerMockito.mock(WorkflowJob.class);
        final WorkflowRun build1 = PowerMockito.spy(new WorkflowRun(j));
        PowerMockito.when(build1.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build1, "X", "V");
        PowerMockito.doReturn(null).when(build1).getPreviousBuild();

        final WorkflowRun build2 = PowerMockito.spy(new WorkflowRun(j));
        PowerMockito.when(build2.getResult()).thenReturn(Result.SUCCESS);
        MockUtilities.addChangeSet(build2, "Z", "V");
        PowerMockito.doReturn(build1).when(build2).getPreviousCompletedBuild();

        final WorkflowRun build3 = PowerMockito.spy(new WorkflowRun(j));
        PowerMockito.when(build3.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build3, "A");
        PowerMockito.doReturn(build2).when(build3).getPreviousCompletedBuild();

        final WorkflowRun build4 = PowerMockito.spy(new WorkflowRun(j));
        PowerMockito.when(build4.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build4, "B");
        PowerMockito.doReturn(build3).when(build4).getPreviousCompletedBuild();

        TestUtilities.checkRecipients(build4, new CulpritsRecipientProvider(), "A", "B");
    }

    @Test
    public void testAddRecipients2() throws Exception {
        final WorkflowJob j = PowerMockito.mock(WorkflowJob.class);
        final WorkflowRun build1 = PowerMockito.spy(new WorkflowRun(j));
        PowerMockito.when(build1.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build1, "X", "V");
        PowerMockito.doReturn(null).when(build1).getPreviousBuild();

        final WorkflowRun build2 = PowerMockito.spy(new WorkflowRun(j));
        PowerMockito.when(build2.getResult()).thenReturn(Result.SUCCESS);
        MockUtilities.addChangeSet(build2, "Z", "V");
        PowerMockito.doReturn(build1).when(build2).getPreviousCompletedBuild();

        TestUtilities.checkRecipients(build2, new CulpritsRecipientProvider(), "X", "V", "Z");
    }
}
