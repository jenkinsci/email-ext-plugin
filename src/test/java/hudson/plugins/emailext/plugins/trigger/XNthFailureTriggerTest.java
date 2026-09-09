package hudson.plugins.emailext.plugins.trigger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.emailext.AttachBuildLogMode;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Kanstantsin Shautsou
 */
class XNthFailureTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        XNthFailureTrigger trigger =
                new XNthFailureTrigger(recProviders, "", "", "", "", "", AttachBuildLogMode.NONE, "project");
        trigger.setRequiredFailureCount(3);
        return trigger;
    }

    @Test
    void testTrigger_success() {
        assertNotTriggered(Result.SUCCESS);
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS);
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS, Result.SUCCESS);
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS, Result.SUCCESS, Result.SUCCESS);
    }

    @Test
    void testTrigger_thirdFailureAfterSuccess() {
        assertTriggered(Result.FAILURE, Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.FAILURE);
    }

    @Test
    void testTrigger_thirdBuildFails() {
        assertTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE);
        assertTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.FAILURE);
    }

    @Test
    void testTrigger_failure() {
        assertNotTriggered(Result.FAILURE);
        assertNotTriggered(Result.FAILURE, Result.FAILURE);
        assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE);
        assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.SUCCESS);
    }

    @Test
    @WithJenkins
    void testConfigRoundTrip(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();

        final ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        XNthFailureTrigger trigger =
                new XNthFailureTrigger(Collections.emptyList(), "", "", "", "", "", AttachBuildLogMode.NONE, "project");
        trigger.setRequiredFailureCount(5);

        publisher.getConfiguredTriggers().add(trigger);
        project.getPublishersList().add(publisher);
        project.save();

        final FreeStyleProject projectAfter = j.configRoundtrip(project);
        assertNotNull(projectAfter);

        final ExtendedEmailPublisher publisherAfter =
                projectAfter.getPublishersList().get(ExtendedEmailPublisher.class);
        assertNotNull(publisherAfter);

        assertTrue(publisherAfter.getConfiguredTriggers().size() == 1);
        assertTrue(publisherAfter.getConfiguredTriggers().get(0) instanceof XNthFailureTrigger);
        final XNthFailureTrigger emailTrigger =
                (XNthFailureTrigger) publisherAfter.getConfiguredTriggers().get(0);

        assertThat(emailTrigger.getRequiredFailureCount(), is(5));
    }
}
