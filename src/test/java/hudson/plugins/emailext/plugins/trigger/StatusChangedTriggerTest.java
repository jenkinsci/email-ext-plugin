package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the "Status changed" trigger.
 *
 * @author francois_ritaly
 */
class StatusChangedTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new StatusChangedTrigger(recProviders, "", "", "", "", "", 0, "project");
    }

    // --- Transitions from <no-status> to <status> --- //
    @Test
    void testTrigger_Success() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.SUCCESS);
    }

    @Test
    void testTrigger_Aborted() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.ABORTED);
    }

    @Test
    void testTrigger_Failure() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.FAILURE);
    }

    @Test
    void testTrigger_NotBuilt() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.NOT_BUILT);
    }

    @Test
    void testTrigger_Unstable() {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.UNSTABLE);
    }

    // --- Transitions between 2 statuses --- //
    // There are 5 possible statuses (success, aborted, failure, not_built and unstable)
    // so we must test 5x5 = 25 transitions
    // Transitions from the "success" status
    @Test
    void testTrigger_SuccessSuccess() {
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS);
    }

    @Test
    void testTrigger_SuccessAborted() {
        assertTriggered(Result.SUCCESS, Result.ABORTED);
    }

    @Test
    void testTrigger_SuccessFailure() {
        assertTriggered(Result.SUCCESS, Result.FAILURE);
    }

    @Test
    void testTrigger_SuccessNotBuilt() {
        assertTriggered(Result.SUCCESS, Result.NOT_BUILT);
    }

    @Test
    void testTrigger_SuccessUnstable() {
        assertTriggered(Result.SUCCESS, Result.UNSTABLE);
    }

    // Transitions from the "aborted" status
    @Test
    void testTrigger_AbortedSuccess() {
        assertTriggered(Result.ABORTED, Result.SUCCESS);
    }

    @Test
    void testTrigger_AbortedAborted() {
        assertNotTriggered(Result.ABORTED, Result.ABORTED);
    }

    @Test
    void testTrigger_AbortedFailure() {
        assertTriggered(Result.ABORTED, Result.FAILURE);
    }

    @Test
    void testTrigger_AbortedNotBuilt() {
        assertTriggered(Result.ABORTED, Result.NOT_BUILT);
    }

    @Test
    void testTrigger_AbortedUnstable() {
        assertTriggered(Result.ABORTED, Result.UNSTABLE);
    }

    // Transitions from the "failure" status
    @Test
    void testTrigger_FailureSuccess() {
        assertTriggered(Result.FAILURE, Result.SUCCESS);
    }

    @Test
    void testTrigger_FailureAborted() {
        assertTriggered(Result.FAILURE, Result.ABORTED);
    }

    @Test
    void testTrigger_FailureFailure() {
        assertNotTriggered(Result.FAILURE, Result.FAILURE);
    }

    @Test
    void testTrigger_FailureNotBuilt() {
        assertTriggered(Result.FAILURE, Result.NOT_BUILT);
    }

    @Test
    void testTrigger_FailureUnstable() {
        assertTriggered(Result.FAILURE, Result.UNSTABLE);
    }

    // Transitions from the "not_built" status
    @Test
    void testTrigger_NotBuiltSuccess() {
        assertTriggered(Result.NOT_BUILT, Result.SUCCESS);
    }

    @Test
    void testTrigger_NotBuiltAborted() {
        assertTriggered(Result.NOT_BUILT, Result.ABORTED);
    }

    @Test
    void testTrigger_NotBuiltFailure() {
        assertTriggered(Result.NOT_BUILT, Result.FAILURE);
    }

    @Test
    void testTrigger_NotBuiltNotBuilt() {
        assertNotTriggered(Result.NOT_BUILT, Result.NOT_BUILT);
    }

    @Test
    void testTrigger_NotBuiltUnstable() {
        assertTriggered(Result.NOT_BUILT, Result.UNSTABLE);
    }

    // Transitions from the "unstable" status
    @Test
    void testTrigger_UnstableSuccess() {
        assertTriggered(Result.UNSTABLE, Result.SUCCESS);
    }

    @Test
    void testTrigger_UnstableAborted() {
        assertTriggered(Result.UNSTABLE, Result.ABORTED);
    }

    @Test
    void testTrigger_UnstableFailure() {
        assertTriggered(Result.UNSTABLE, Result.FAILURE);
    }

    @Test
    void testTrigger_UnstableNotBuilt() {
        assertTriggered(Result.UNSTABLE, Result.NOT_BUILT);
    }

    @Test
    void testTrigger_UnstableUnstable() {
        assertNotTriggered(Result.UNSTABLE, Result.UNSTABLE);
    }
}
