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
        FreeStyleProject.class
})
public class DevelopersRecipientProviderTest {

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
    public void testAddRecipients() throws Exception {
        final FreeStyleProject p = PowerMockito.mock(FreeStyleProject.class);
        final FreeStyleBuild build1 = PowerMockito.spy(new FreeStyleBuild(p));
        PowerMockito.doReturn(Result.UNSTABLE).when(build1).getResult();
        MockUtilities.addRequestor(build1, "A");
        MockUtilities.addChangeSet(build1, "X", "V");
        TestUtilities.checkRecipients(build1, new DevelopersRecipientProvider(), "X", "V");

        final WorkflowJob j = PowerMockito.mock(WorkflowJob.class);
        final WorkflowRun build2 = PowerMockito.spy(new WorkflowRun(j));
        PowerMockito.doReturn(Result.UNSTABLE).when(build2).getResult();
        MockUtilities.addChangeSet(build2, "X", "V");
        TestUtilities.checkRecipients(build2, new DevelopersRecipientProvider(), "X", "V");
    }
}
