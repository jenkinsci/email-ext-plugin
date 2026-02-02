package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.jupiter.api.Test;

class SecondFailureTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new SecondFailureTrigger(recProviders, "", "", "", "", "", 0, "project");
    }

    @Test
    void testTrigger_success() {
        assertNotTriggered(Result.SUCCESS);
    }

    @Test
    void testTrigger_firstFailureAfterSuccess() {
        assertNotTriggered(Result.SUCCESS, Result.FAILURE);
    }

    @Test
    void testTrigger_secondFailureAfterSuccess() {
        assertTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE);
        assertTriggered(Result.FAILURE, Result.FAILURE, Result.SUCCESS, Result.FAILURE, Result.FAILURE);
    }

    @Test
    void testTrigger_thirdFailureAfterSuccess() {
        assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.FAILURE);
    }

    @Test
    void testTrigger_firstBuildFails() {
        assertNotTriggered(Result.FAILURE);
    }

    @Test
    void testTrigger_firstTwoBuildsFail() {
        assertTriggered(Result.FAILURE, Result.FAILURE);
    }

    @Test
    void testTrigger_firstThreeBuildsFail() {
        assertNotTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE);
    }
}
