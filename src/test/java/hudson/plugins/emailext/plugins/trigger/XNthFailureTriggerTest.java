package hudson.plugins.emailext.plugins.trigger;

import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.Test;

import java.io.IOException;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;

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
    public void testTrigger_success()
            throws IOException, InterruptedException {
        assertNotTriggered(SUCCESS);
        assertNotTriggered(SUCCESS, SUCCESS);
        assertNotTriggered(SUCCESS, SUCCESS, SUCCESS);
        assertNotTriggered(SUCCESS, SUCCESS, SUCCESS, SUCCESS);
    }

    @Test
    public void testTrigger_thirdFailureAfterSuccess()
            throws IOException, InterruptedException {
        assertTriggered(FAILURE, SUCCESS, FAILURE, FAILURE, FAILURE);
    }

    @Test
    public void testTrigger_thirdBuildFails()
            throws IOException, InterruptedException {
        assertTriggered(FAILURE, FAILURE, FAILURE);
        assertTriggered(SUCCESS, FAILURE, FAILURE, FAILURE);
    }

    @Test
    public void testTrigger_failure()
            throws IOException, InterruptedException {
        assertNotTriggered(FAILURE);
        assertNotTriggered(FAILURE, FAILURE);
        assertNotTriggered(SUCCESS, FAILURE, FAILURE);
        assertNotTriggered(SUCCESS, FAILURE, FAILURE, SUCCESS);
    }
}
