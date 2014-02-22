package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Adrien Lecharpentier <adrien.lecharpentier@zenika.com>
 */
public class FirstUnstableTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new FirstUnstableTrigger(recProviders, "", "", "", "", "", 0, "");
    }

    @Test
    public void testTrigger_unstable()
            throws IOException, InterruptedException {
        assertTriggered(Result.UNSTABLE);
    }

    @Test
    public void testTrigger_success()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS);
    }

    @Test
    public void testTrigger_failure()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.FAILURE);
    }

    @Test
    public void testTrigger_failureUnstable()
            throws IOException, InterruptedException {
        assertTriggered(Result.FAILURE, Result.UNSTABLE);
    }

    @Test
    public void testTrigger_multipleFailure()
            throws IOException, InterruptedException {
        assertTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE, Result.UNSTABLE);
    }

    @Test
    public void testTrigger_failureSuccess()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.FAILURE, Result.SUCCESS);
    }

    @Test
    public void testTrigger_failureSuccessUnstable()
            throws IOException, InterruptedException {
        assertTriggered(Result.FAILURE, Result.SUCCESS, Result.UNSTABLE);
    }

    @Test
    public void testTrigger_unstableUnstable()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.UNSTABLE, Result.UNSTABLE);
    }
}
