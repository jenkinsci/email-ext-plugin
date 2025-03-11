package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.jupiter.api.Test;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 */
class FirstUnstableTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new FirstUnstableTrigger(recProviders, "", "", "", "", "", 0, "");
    }

    @Test
    void testTrigger_unstable() {
        assertTriggered(Result.UNSTABLE);
    }

    @Test
    void testTrigger_success() {
        assertNotTriggered(Result.SUCCESS);
    }

    @Test
    void testTrigger_failure() {
        assertNotTriggered(Result.FAILURE);
    }

    @Test
    void testTrigger_failureUnstable() {
        assertTriggered(Result.FAILURE, Result.UNSTABLE);
    }

    @Test
    void testTrigger_multipleFailure() {
        assertTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE, Result.UNSTABLE);
    }

    @Test
    void testTrigger_failureSuccess() {
        assertNotTriggered(Result.FAILURE, Result.SUCCESS);
    }

    @Test
    void testTrigger_failureSuccessUnstable() {
        assertTriggered(Result.FAILURE, Result.SUCCESS, Result.UNSTABLE);
    }

    @Test
    void testTrigger_unstableUnstable() {
        assertNotTriggered(Result.UNSTABLE, Result.UNSTABLE);
    }
}
