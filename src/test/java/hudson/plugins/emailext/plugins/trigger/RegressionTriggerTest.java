package hudson.plugins.emailext.plugins.trigger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import org.junit.jupiter.api.Test;

public class RegressionTriggerTest {

    @Test
    void testTriggerReturnsFalseWhenNoTestResults() {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        TaskListener listener = mock(TaskListener.class);

        // Simulate no previous build
        when(build.getResult()).thenReturn(Result.SUCCESS);

        // Simulate no test results (IMPORTANT path)
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(null);

        RegressionTrigger trigger = new RegressionTrigger(null, "", "", "", "", "", 0, "");

        boolean result = trigger.trigger(build, listener);

        assertFalse(result);
    }
}
