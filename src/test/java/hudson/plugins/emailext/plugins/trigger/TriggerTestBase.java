package hudson.plugins.emailext.plugins.trigger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.util.StreamTaskListener;
import java.util.Collections;
import java.util.List;

/**
 * Base class for testing
 */
public abstract class TriggerTestBase {
    
    protected List<RecipientProvider> recProviders = Collections.emptyList();

    abstract EmailTrigger newInstance();
    
    TaskListener getTaskListener() {
        return StreamTaskListener.fromStdout();
    }

    /**
     * Asserts the the specified result history triggers the EmailTrigger.
     */
    void assertTriggered(Result... resultHistory) {
        EmailTrigger trigger = newInstance();
        AbstractBuild<?, ?> build = mockBuild(resultHistory);
        assertTrue(trigger.trigger(build, getTaskListener()));
    }

    /**
     * Asserts the the specified result history does not trigger the
     * EmailTrigger.
     */
    void assertNotTriggered(Result... resultHistory) {
        EmailTrigger trigger = newInstance();
        AbstractBuild<?, ?> build = mockBuild(resultHistory);
        assertFalse(trigger.trigger(build, getTaskListener()));
    }

    /**
     * Creates a mock AbstractBuild with the specified history of results.
     */
    AbstractBuild<?, ?> mockBuild(Result... resultHistory) {
        FreeStyleBuild toRet = mock(FreeStyleBuild.class);

        FreeStyleBuild build = toRet;
        for (int i = resultHistory.length - 1; i >= 0; i--) {
            when(build.getResult()).thenReturn(resultHistory[i]);

            if (i != 0) {
                FreeStyleBuild prevBuild = mock(FreeStyleBuild.class);
                when(build.getPreviousBuild()).thenReturn(prevBuild);
                build = prevBuild;
            }
        }

        return toRet;
    }
}
