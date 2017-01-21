package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.Test;

import java.io.IOException;

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
    public void testTrigger_Success() throws IOException, InterruptedException {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.SUCCESS);
    }

    @Test
    public void testTrigger_Aborted() throws IOException, InterruptedException {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.ABORTED);
    }

    @Test
    public void testTrigger_Failure() throws IOException, InterruptedException {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.FAILURE);
    }

    @Test
    public void testTrigger_NotBuilt() throws IOException, InterruptedException {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_Unstable() throws IOException, InterruptedException {
        // Notification expected since this is the first build status defined
        assertTriggered(Result.UNSTABLE);
    }

    // --- Transitions between 2 statuses --- //
    // There are 5 possible statuses (success, aborted, failure, not_built and unstable)
    // so we must test 5x5 = 25 transitions
    // Transitions from the "success" status
    @Test
    public void testTrigger_SuccessSuccess() throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS);
    }

    @Test
    public void testTrigger_SuccessAborted() throws IOException, InterruptedException {
        assertTriggered(Result.SUCCESS, Result.ABORTED);
    }

    @Test
    public void testTrigger_SuccessFailure() throws IOException, InterruptedException {
        assertTriggered(Result.SUCCESS, Result.FAILURE);
    }

    @Test
    public void testTrigger_SuccessNotBuilt() throws IOException, InterruptedException {
        assertTriggered(Result.SUCCESS, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_SuccessUnstable() throws IOException, InterruptedException {
        assertTriggered(Result.SUCCESS, Result.UNSTABLE);
    }

    // Transitions from the "aborted" status
    @Test
    public void testTrigger_AbortedSuccess() throws IOException, InterruptedException {
        assertTriggered(Result.ABORTED, Result.SUCCESS);
    }

    @Test
    public void testTrigger_AbortedAborted() throws IOException, InterruptedException {
        assertNotTriggered(Result.ABORTED, Result.ABORTED);
    }

    @Test
    public void testTrigger_AbortedFailure() throws IOException, InterruptedException {
        assertTriggered(Result.ABORTED, Result.FAILURE);
    }

    @Test
    public void testTrigger_AbortedNotBuilt() throws IOException, InterruptedException {
        assertTriggered(Result.ABORTED, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_AbortedUnstable() throws IOException, InterruptedException {
        assertTriggered(Result.ABORTED, Result.UNSTABLE);
    }

    // Transitions from the "failure" status
    @Test
    public void testTrigger_FailureSuccess() throws IOException, InterruptedException {
        assertTriggered(Result.FAILURE, Result.SUCCESS);
    }

    @Test
    public void testTrigger_FailureAborted() throws IOException, InterruptedException {
        assertTriggered(Result.FAILURE, Result.ABORTED);
    }

    @Test
    public void testTrigger_FailureFailure() throws IOException, InterruptedException {
        assertNotTriggered(Result.FAILURE, Result.FAILURE);
    }

    @Test
    public void testTrigger_FailureNotBuilt() throws IOException, InterruptedException {
        assertTriggered(Result.FAILURE, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_FailureUnstable() throws IOException, InterruptedException {
        assertTriggered(Result.FAILURE, Result.UNSTABLE);
    }

    // Transitions from the "not_built" status
    @Test
    public void testTrigger_NotBuiltSuccess() throws IOException, InterruptedException {
        assertTriggered(Result.NOT_BUILT, Result.SUCCESS);
    }

    @Test
    public void testTrigger_NotBuiltAborted() throws IOException, InterruptedException {
        assertTriggered(Result.NOT_BUILT, Result.ABORTED);
    }

    @Test
    public void testTrigger_NotBuiltFailure() throws IOException, InterruptedException {
        assertTriggered(Result.NOT_BUILT, Result.FAILURE);
    }

    @Test
    public void testTrigger_NotBuiltNotBuilt() throws IOException, InterruptedException {
        assertNotTriggered(Result.NOT_BUILT, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_NotBuiltUnstable() throws IOException, InterruptedException {
        assertTriggered(Result.NOT_BUILT, Result.UNSTABLE);
    }

    // Transitions from the "unstable" status
    @Test
    public void testTrigger_UnstableSuccess() throws IOException, InterruptedException {
        assertTriggered(Result.UNSTABLE, Result.SUCCESS);
    }

    @Test
    public void testTrigger_UnstableAborted() throws IOException, InterruptedException {
        assertTriggered(Result.UNSTABLE, Result.ABORTED);
    }

    @Test
    public void testTrigger_UnstableFailure() throws IOException, InterruptedException {
        assertTriggered(Result.UNSTABLE, Result.FAILURE);
    }

    @Test
    public void testTrigger_UnstableNotBuilt() throws IOException, InterruptedException {
        assertTriggered(Result.UNSTABLE, Result.NOT_BUILT);
    }

    @Test
    public void testTrigger_UnstableUnstable() throws IOException, InterruptedException {
        assertNotTriggered(Result.UNSTABLE, Result.UNSTABLE);
    }
}
