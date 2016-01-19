package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;

import java.io.IOException;

import org.junit.Test;

public class SecondFailureTriggerTest extends AbstractTriggerTestBase {

    @Override
    hudson.plugins.emailext.plugins.AbstractEmailTrigger newInstance() {
        return new SecondFailureTrigger(recProviders, "", "", "", "", "", 0, "project");
    }

    @Test
    public void testTrigger_success()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS);
    }

    @Test
    public void testTrigger_firstFailureAfterSuccess()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS, Result.FAILURE);
    }

    @Test
    public void testTrigger_secondFailureAfterSuccess()
            throws IOException, InterruptedException {
        assertTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE);
        assertTriggered(Result.FAILURE, Result.FAILURE, Result.SUCCESS, Result.FAILURE, Result.FAILURE);
    }

    @Test
    public void testTrigger_thirdFailureAfterSuccess()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.FAILURE);
    }

    @Test
    public void testTrigger_firstBuildFails()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.FAILURE);
    }

    @Test
    public void testTrigger_firstTwoBuildsFail()
            throws IOException, InterruptedException {
        assertTriggered(Result.FAILURE, Result.FAILURE);
    }

    @Test
    public void testTrigger_firstThreeBuildsFail()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE);
    }
}
