package hudson.plugins.emailext.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.RequesterRecipientProvider;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RecipientProviderTest {

    @Test
    void allSupporting(JenkinsRule j) {
        List<RecipientProviderDescriptor> descriptors = RecipientProvider.allSupporting(WorkflowJob.class);
        assertThat(
                descriptors, CoreMatchers.hasItem(CoreMatchers.isA(DevelopersRecipientProvider.DescriptorImpl.class)));
        assertThat(
                descriptors,
                CoreMatchers.not(CoreMatchers.hasItem(CoreMatchers.isA(ListRecipientProvider.DescriptorImpl.class))));

        descriptors = RecipientProvider.allSupporting(FreeStyleProject.class);
        assertThat(
                descriptors, CoreMatchers.hasItem(CoreMatchers.isA(DevelopersRecipientProvider.DescriptorImpl.class)));
        assertThat(descriptors, CoreMatchers.hasItem(CoreMatchers.isA(ListRecipientProvider.DescriptorImpl.class)));
    }

    @Test
    void checkAllSupport(JenkinsRule j) {
        RecipientProvider.checkAllSupport(
                Arrays.asList(new RequesterRecipientProvider(), new DevelopersRecipientProvider()), WorkflowJob.class);
        List<? extends RecipientProvider> providers =
                Arrays.asList(new RequesterRecipientProvider(), new ListRecipientProvider());
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> RecipientProvider.checkAllSupport(providers, WorkflowJob.class));
        assertEquals(
                MessageFormat.format(
                        "The following recipient providers do not support {0} {1}",
                        WorkflowJob.class.getName(), ListRecipientProvider.class.getName()),
                ex.getMessage());
    }
}
