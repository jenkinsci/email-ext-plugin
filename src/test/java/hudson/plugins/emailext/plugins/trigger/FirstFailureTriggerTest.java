package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.jupiter.api.Test;

class FirstFailureTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new FirstFailureTrigger(recProviders, "", "", "", "", "", 0, "project");
    }

    @Test
    void testTrigger_success() {
        assertNotTriggered(Result.SUCCESS);
    }

    @Test
    void testTrigger_multipleSuccess() {
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS, Result.SUCCESS);
    }

    @Test
    void testTrigger_firstFailureAfterSuccess() {
        assertTriggered(Result.SUCCESS, Result.FAILURE);
        assertTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE, Result.SUCCESS, Result.FAILURE);
    }

    @Test
    void testTrigger_secondFailureAfterSuccess() {
        assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE);
    }

    @Test
    void testTrigger_firstBuildFails() {
        assertTriggered(Result.FAILURE);
    }

    @Test
    void testTrigger_firstTwoBuildsFail() {
        assertNotTriggered(Result.FAILURE, Result.FAILURE);
    }
}
