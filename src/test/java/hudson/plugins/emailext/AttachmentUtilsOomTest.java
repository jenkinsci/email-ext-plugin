package hudson.plugins.emailext;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.RealJenkinsExtension;

class AttachmentUtilsOomTest {

    @RegisterExtension
    static final RealJenkinsExtension rj = new RealJenkinsExtension().javaOptions("-Xmx256m");

    @Test
    void largeBuildLogDoesNotExhaustMemory() throws Throwable {
        rj.then(AttachmentUtilsOomTest::_largeBuildLogDoesNotExhaustMemory);
    }

    public static final class LargeLogBuilder extends TestBuilder {
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            String line = "The sky above the port was the color of television tuned to a dead channel.";
            int iterations = (150 * 1024 * 1024) / line.length();
            for (int i = 0; i < iterations; i++) {
                listener.getLogger().println(line);
            }
            return true;
        }
    }

    private static void _largeBuildLogDoesNotExhaustMemory(JenkinsRule r) throws Exception {
        FreeStyleProject project = r.createFreeStyleProject("large-log");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachBuildLog = true;
        publisher.compressBuildLog = true;
        publisher.recipientList = "morgan@blackhand.com";
        SuccessTrigger trigger = new SuccessTrigger(
                Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        publisher.getConfiguredTriggers().add(trigger);
        project.getPublishersList().add(publisher);
        project.getBuildersList().add(new LargeLogBuilder());

        // Without the streaming fix, getInputStream() buffers the entire log in a ByteArrayOutputStream
        // causing an OutOfMemoryError at -Xmx256m. The streaming fix reads through a 64KB pipe
        // buffer so this completes without exhausting the heap.
        r.buildAndAssertSuccess(project);
    }
}
