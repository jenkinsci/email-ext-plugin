package hudson.plugins.emailext.plugins.content;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class WorkspaceFileContentTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test1() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        TaskListener listener = StreamTaskListener.fromStdout();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("foo").write("Hello, world!", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        WorkspaceFileContent content = new WorkspaceFileContent();
        content.path = "foo";
        assertEquals("Hello, world!", content.evaluate(build, listener, WorkspaceFileContent.MACRO_NAME));
        content.path = "no-such-file";
        assertEquals("ERROR: File 'no-such-file' does not exist", content.evaluate(build, listener, WorkspaceFileContent.MACRO_NAME));
        content.fileNotFoundMessage = "No '%s' for you!";
        assertEquals("No 'no-such-file' for you!", content.evaluate(build, listener, WorkspaceFileContent.MACRO_NAME));
    }
}
