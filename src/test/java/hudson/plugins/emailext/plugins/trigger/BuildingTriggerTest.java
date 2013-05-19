package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.util.StreamTaskListener;

import java.io.IOException;

import org.junit.Test;

/**
 *
 * @author acearl
 */
public class BuildingTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new BuildingTrigger();
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
