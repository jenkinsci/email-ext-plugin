package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.Test;

/**
 * Unit tests for the "Status changed" trigger.
 *
 * @author francois_ritaly
 */
public class StatusChangedTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new StatusChangedTrigger(recProviders, "", "", "", "", "", 0, "project");
    }

    // --- Transitions from <no-status> to <status> --- //
    @Test
    public void testTrigger_Success() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.SUCCESS);
    }

    @Test
    public void testTrigger_Aborted() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.ABORTED);
    }

    @Test
    public void testTrigger_Failure() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.FAILURE);
    }

    @Test
    public void testTrigger_NotBuilt() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_Unstable() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.UNSTABLE);
    }

    // --- Transitions between 2 statuses --- //
    // There are 5 possible statuses (success, aborted, failure, not_built and unstable)
    // so we must test 5x5 = 25 transitions
    // Transitions from the "success" status
    @Test
    public void testTrigger_SuccessSuccess() {
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS);
    }

    @Test
    public void testTrigger_SuccessAborted() {
        assertTriggered(Result.SUCCESS, Result.ABORTED);
    }

    @Test
    public void testTrigger_SuccessFailure() {
        assertTriggered(Result.SUCCESS, Result.FAILURE);
    }

    @Test
    public void testTrigger_SuccessNotBuilt() {
        assertTriggered(Result.SUCCESS, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_SuccessUnstable() {
        assertTriggered(Result.SUCCESS, Result.UNSTABLE);
    }

    // Transitions from the "aborted" status
    @Test
    public void testTrigger_AbortedSuccess() {
        assertTriggered(Result.ABORTED, Result.SUCCESS);
    }

    @Test
    public void testTrigger_AbortedAborted() {
        assertNotTriggered(Result.ABORTED, Result.ABORTED);
    }

    @Test
    public void testTrigger_AbortedFailure() {
        assertTriggered(Result.ABORTED, Result.FAILURE);
    }

    @Test
    public void testTrigger_AbortedNotBuilt() {
        assertTriggered(Result.ABORTED, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_AbortedUnstable() {
        assertTriggered(Result.ABORTED, Result.UNSTABLE);
    }

    // Transitions from the "failure" status
    @Test
    public void testTrigger_FailureSuccess() {
        assertTriggered(Result.FAILURE, Result.SUCCESS);
    }

    @Test
    public void testTrigger_FailureAborted() {
        assertTriggered(Result.FAILURE, Result.ABORTED);
    }

    @Test
    public void testTrigger_FailureFailure() {
        assertNotTriggered(Result.FAILURE, Result.FAILURE);
    }

    @Test
    public void testTrigger_FailureNotBuilt() {
        assertTriggered(Result.FAILURE, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_FailureUnstable() {
        assertTriggered(Result.FAILURE, Result.UNSTABLE);
    }

    // Transitions from the "not_built" status
    @Test
    public void testTrigger_NotBuiltSuccess() {
        assertTriggered(Result.NOT_BUILT, Result.SUCCESS);
    }

    @Test
    public void testTrigger_NotBuiltAborted() {
        assertTriggered(Result.NOT_BUILT, Result.ABORTED);
    }

    @Test
    public void testTrigger_NotBuiltFailure() {
        assertTriggered(Result.NOT_BUILT, Result.FAILURE);
    }

    @Test
    public void testTrigger_NotBuiltNotBuilt() {
        assertNotTriggered(Result.NOT_BUILT, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_NotBuiltUnstable() {
        assertTriggered(Result.NOT_BUILT, Result.UNSTABLE);
    }

    // Transitions from the "unstable" status
    @Test
    public void testTrigger_UnstableSuccess() {
        assertTriggered(Result.UNSTABLE, Result.SUCCESS);
    }

    @Test
    public void testTrigger_UnstableAborted() {
        assertTriggered(Result.UNSTABLE, Result.ABORTED);
    }

    @Test
    public void testTrigger_UnstableFailure() {
        assertTriggered(Result.UNSTABLE, Result.FAILURE);
    }

    @Test
    public void testTrigger_UnstableNotBuilt() {
        assertTriggered(Result.UNSTABLE, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_UnstableUnstable() {
        assertNotTriggered(Result.UNSTABLE, Result.UNSTABLE);
    }
}
