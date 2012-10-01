package hudson.plugins.emailext.plugins.content;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Kohsuke Kawaguchi
 */
public class WorkspaceFileContentTest extends HudsonTestCase {
    public void test1() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("foo").write("Hello, world!","UTF-8");
                return true;
            }
        });
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        WorkspaceFileContent content = new WorkspaceFileContent();
        assertEquals("Hello, world!", content.getContent(build, null, null, Collections.singletonMap("path", "foo")));
	assertEquals("ERROR: File 'no-such-file' does not exist", content.getContent(build, null, null, Collections.singletonMap("path", "no-such-file")));
    }
}
