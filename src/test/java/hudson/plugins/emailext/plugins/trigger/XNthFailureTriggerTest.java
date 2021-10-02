package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.Test;

/**
 * @author Kanstantsin Shautsou
 */
public class XNthFailureTriggerTest extends TriggerTestBase {
    @Override
    EmailTrigger newInstance() {
        XNthFailureTrigger trigger = new XNthFailureTrigger(recProviders, "", "", "", "", "", 0, "project");
        trigger.setRequiredFailureCount(3);
        return trigger;
    }

    @Test
    public void testTrigger_success() {
        assertNotTriggered(Result.SUCCESS);
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS);
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS, Result.SUCCESS);
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS, Result.SUCCESS, Result.SUCCESS);
    }

    @Test
    public void testTrigger_thirdFailureAfterSuccess() {
        assertTriggered(Result.FAILURE, Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.FAILURE);
    }

    @Test
    public void testTrigger_thirdBuildFails() {
        assertTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE);
        assertTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.FAILURE);
    }

    @Test
    public void testTrigger_failure() {
        assertNotTriggered(Result.FAILURE);
        assertNotTriggered(Result.FAILURE, Result.FAILURE);
        assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE);
        assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.SUCCESS);
    }
}
