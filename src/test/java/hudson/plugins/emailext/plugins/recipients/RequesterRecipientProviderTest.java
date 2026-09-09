package hudson.plugins.emailext.plugins.recipients;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.PrintStream;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RequesterRecipientProviderTest {

    @Test
    void addRecipients_doesNotThrowWhenUpstreamChainResolvesToNull() {
        Run<?, ?> run = mock(Run.class);
        when(run.getCauses()).thenReturn(Collections.emptyList());

        TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(mock(PrintStream.class));

        assertDoesNotThrow(() -> {
            RecipientProviderUtilities.getUserTriggeringTheBuild(run);
        });
    }
}
