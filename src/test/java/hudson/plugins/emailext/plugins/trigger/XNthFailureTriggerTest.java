package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.jupiter.api.Test;

/**
 * @author Kanstantsin Shautsou
 */
class XNthFailureTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        XNthFailureTrigger trigger = new XNthFailureTrigger(recProviders, "", "", "", "", "", 0, "project");
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
}
