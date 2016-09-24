package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.Test;

import java.io.IOException;

/**
 *
 * @author acearl
 */
public class BuildingTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new BuildingTrigger(recProviders, "", "", "", "", "", 0, "project");
    }
    
    @Test
    public void testTrigger_success() 
            throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS);
    }
    
    @Test
    public void testTrigger_failure() 
            throws IOException, InterruptedException {
        assertNotTriggered(Result.SUCCESS);
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
        assertNotTriggered(Result.FAILURE, Result.SUCCESS, Result.UNSTABLE);
    }
}
