package hudson.plugins.emailext.plugins.trigger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author Kanstantsin Shautsou
 */
public class XNthFailureTriggerJTest {
    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Test
    public void testConfigRoundTrip() throws Exception {
        FreeStyleProject project = jRule.createFreeStyleProject();

        final ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        XNthFailureTrigger trigger = new XNthFailureTrigger(Collections.emptyList(),
                "", "", "", "", "", 0, "project");
        trigger.setRequiredFailureCount(5);

        publisher.configuredTriggers.add(trigger);
        project.getPublishersList().add(publisher);
        project.save();

        jRule.configRoundtrip(project);

        final FreeStyleProject projectAfter = (FreeStyleProject) jRule.getInstance().getItem(project.getName());

        final ExtendedEmailPublisher rPublisher = projectAfter.getPublishersList().get(ExtendedEmailPublisher.class);

        final XNthFailureTrigger emailTrigger = (XNthFailureTrigger) rPublisher.getConfiguredTriggers().get(0);

        assertThat(emailTrigger.getRequiredFailureCount(), is(5));
    }
}
