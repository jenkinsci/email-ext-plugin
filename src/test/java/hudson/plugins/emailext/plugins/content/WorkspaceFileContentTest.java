package hudson.plugins.emailext.plugins.content;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class WorkspaceFileContentTest extends HudsonTestCase {
    public void test1() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        TaskListener listener = new StreamTaskListener(System.out);
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("foo").write("Hello, world!","UTF-8");
                return true;
            }
        });
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        WorkspaceFileContent content = new WorkspaceFileContent();
        content.path = "foo";
        assertEquals("Hello, world!", content.evaluate(build, listener, WorkspaceFileContent.MACRO_NAME));
        content.path = "no-such-file";
		assertEquals("ERROR: File 'no-such-file' does not exist", content.evaluate(build, listener, WorkspaceFileContent.MACRO_NAME));
    }
}
