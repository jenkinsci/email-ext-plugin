package hudson.plugins.emailext.plugins.recipients;

import hudson.model.Result;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import jenkins.scm.api.metadata.ContributorMetadataAction;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ContributorMetadataRecipientProviderTest {

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
        final WorkflowJob j = Mockito.mock(WorkflowJob.class);
        final WorkflowRun build = Mockito.spy(new WorkflowRun(j));
        Mockito.when(build.getResult()).thenReturn(Result.UNSTABLE);
        final ContributorMetadataAction action = Mockito.mock(ContributorMetadataAction.class);
        Mockito.when(action.getContributorEmail()).thenReturn("mickey@disney.com");
        Mockito.when(build.getAction(ContributorMetadataAction.class)).thenReturn(action);

        TestUtilities.checkRecipients(build, new ContributorMetadataRecipientProvider(), "mickey@disney.com");
    }    
}
