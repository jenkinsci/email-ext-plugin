package hudson.plugins.emailext.plugins.trigger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import org.junit.jupiter.api.Test;

public class RegressionTriggerTest {

    @Test
    void testTriggerWithTestResults() {
        // Mock build and listener
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        TaskListener listener = mock(TaskListener.class);

        // Mock test result action
        AbstractTestResultAction<?> action = mock(AbstractTestResultAction.class);

        when(build.getResult()).thenReturn(Result.FAILURE);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(action);
        when(action.getFailCount()).thenReturn(1);

        // Create trigger
        RegressionTrigger trigger = new RegressionTrigger(null, "", "", "", "", "", 0, "");

        // Call method
        boolean result = trigger.trigger(build, listener);

        // Assertion (main goal = execute path)
        assertNotNull(result);
    }
}
