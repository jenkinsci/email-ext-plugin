package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.util.XStream2;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class FirstFailureTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new FirstFailureTrigger(recProviders, "", "", "", "", "", 0, "project");
    }

    @Test
    public void testTrigger_success()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS);
    }

    @Test
    public void testTrigger_multipleSuccess()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS, Result.SUCCESS, Result.SUCCESS);
    }

    @Test
    public void testTrigger_firstFailureAfterSuccess()
            throws IOException, InterruptedException {
        assertTriggered(Result.SUCCESS, Result.FAILURE);
        assertTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE, Result.SUCCESS, Result.FAILURE);
    }

    @Test
    public void testTrigger_secondFailureAfterSuccess()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE);
    }

    @Test
    public void testTrigger_firstBuildFails()
            throws IOException, InterruptedException {
        assertTriggered(Result.FAILURE);
    }

    @Test
    public void testTrigger_firstTwoBuildsFail()
            throws IOException, InterruptedException {
        assertNotTriggered(Result.FAILURE, Result.FAILURE);
    }
}
