package hudson.plugins.emailext.plugins.recipients;

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
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
        WorkflowRun.class
})
public class CulpritsRecipientProviderTest {

    @Before
    public void before() throws Exception {
        final Jenkins jenkins = PowerMockito.mock(Jenkins.class);
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
        final WorkflowRun build1 = PowerMockito.mock(WorkflowRun.class);
        PowerMockito.when(build1.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build1, "X", "V");

        final WorkflowRun build2 = PowerMockito.mock(WorkflowRun.class);
        PowerMockito.when(build2.getResult()).thenReturn(Result.SUCCESS);
        MockUtilities.addChangeSet(build2, "Z", "V");
        PowerMockito.when(build2.getPreviousCompletedBuild()).thenReturn(build1);

        final WorkflowRun build3 = PowerMockito.mock(WorkflowRun.class);
        PowerMockito.when(build3.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build3, "A");
        PowerMockito.when(build3.getPreviousCompletedBuild()).thenReturn(build2);

        final WorkflowRun build4 = PowerMockito.mock(WorkflowRun.class);
        PowerMockito.when(build4.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build4, "B");
        PowerMockito.when(build4.getPreviousCompletedBuild()).thenReturn(build3);

        TestUtilities.checkRecipients(build4, new CulpritsRecipientProvider(), "A", "B");
    }

    @Test
    public void testAddRecipients2() throws Exception {
        final WorkflowRun build1 = PowerMockito.mock(WorkflowRun.class);
        PowerMockito.when(build1.getResult()).thenReturn(Result.UNSTABLE);
        MockUtilities.addChangeSet(build1, "X", "V");

        final WorkflowRun build2 = PowerMockito.mock(WorkflowRun.class);
        PowerMockito.when(build2.getResult()).thenReturn(Result.SUCCESS);
        MockUtilities.addChangeSet(build2, "Z", "V");
        PowerMockito.when(build2.getPreviousCompletedBuild()).thenReturn(build1);

        TestUtilities.checkRecipients(build2, new CulpritsRecipientProvider(), "X", "V", "Z");
    }
}
