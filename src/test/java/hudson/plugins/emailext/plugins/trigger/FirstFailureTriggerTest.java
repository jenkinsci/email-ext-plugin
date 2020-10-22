package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.Test;

public class FirstFailureTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new FirstFailureTrigger(recProviders, "", "", "", "", "", 0, "project");
    }

    @Test
    public void testTrigger_success() {
        assertNotTriggered(Result.SUCCESS);
    }

    @Test
    public void testTrigger_multipleSuccess() {
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS, Result.SUCCESS);
    }

    @Test
    public void testTrigger_firstFailureAfterSuccess() {
        assertTriggered(Result.SUCCESS, Result.FAILURE);
        assertTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE, Result.SUCCESS, Result.FAILURE);
    }

    @Test
    public void testTrigger_secondFailureAfterSuccess() {
        assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE);
    }

    @Test
    public void testTrigger_firstBuildFails() {
        assertTriggered(Result.FAILURE);
    }

    @Test
    public void testTrigger_firstTwoBuildsFail() {
        assertNotTriggered(Result.FAILURE, Result.FAILURE);
    }
}
