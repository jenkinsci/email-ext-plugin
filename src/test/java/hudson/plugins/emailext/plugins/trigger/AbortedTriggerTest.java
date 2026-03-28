package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AbortedTriggerTest extends TriggerTestBase {

    @Override
    EmailTrigger newInstance() {
        return new AbortedTrigger(recProviders, "", "", "", "", "", 0, "project");
    }

    @Test
    @DisplayName("Should not trigger when build succeeds")
    void testTrigger_success() {
        assertNotTriggered(Result.SUCCESS);
    }

    @Test
    @DisplayName("Should not trigger when build fails")
    void testTrigger_failure() {
        assertNotTriggered(Result.FAILURE);
    }

    @Test
    @DisplayName("Should not trigger when build is unstable")
    void testTrigger_unstable() {
        assertNotTriggered(Result.UNSTABLE);
    }

    @Test
    @DisplayName("Should trigger when build is aborted")
    void testTrigger_aborted() {
        assertTriggered(Result.ABORTED);
    }

    @Test
    @DisplayName("Should trigger when aborted after previous success")
    void testTrigger_abortedAfterSuccess() {
        assertTriggered(Result.SUCCESS, Result.ABORTED);
    }

    @Test
    @DisplayName("Should trigger on consecutive aborted builds")
    void testTrigger_multipleAborted() {
        assertTriggered(Result.ABORTED, Result.ABORTED);
    }
}