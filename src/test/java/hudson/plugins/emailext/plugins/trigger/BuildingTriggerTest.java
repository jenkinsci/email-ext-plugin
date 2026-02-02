package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.jupiter.api.Test;

/**
 *
 * @author acearl
 */
class BuildingTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new BuildingTrigger(recProviders, "", "", "", "", "", 0, "project");
    }

    @Test
    void testTrigger_success() {
        assertNotTriggered(Result.SUCCESS);
    }

    @Test
    void testTrigger_failure() {
        assertNotTriggered(Result.SUCCESS);
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
        assertNotTriggered(Result.FAILURE, Result.SUCCESS, Result.UNSTABLE);
    }
}
