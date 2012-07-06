package hudson.plugins.emailext.plugins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.plugins.content.FailedTestsContent;
import hudson.tasks.junit.JUnitResultArchiver;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

public class OnlyRegressionsTest extends HudsonTestCase {

    public void testOnlyRegressionsAreShown() throws Exception {
        FreeStyleProject project = createFreeStyleProject("onlyRegressions");
        project.getPublishersList().add(new JUnitResultArchiver("target/testreports/*.xml", true, null));

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
                final URL failedTestReport = OnlyRegressionsTest.class.getClassLoader().getResource("hudson/plugins/emailext/testreports/failed_test.xml");
                FilePath workspace = abstractBuild.getWorkspace();

                FilePath testDir = workspace.child("target").child("testreports");
                testDir.mkdirs();
                FilePath reportFile = testDir.child("failed_test.xml");
                reportFile.copyFrom(failedTestReport);

                return true;
            }
        });
        project.scheduleBuild2(0).get();
        FailedTestsContent failedTestsContent = new FailedTestsContent();
        String content = failedTestsContent.getContent(project.getLastBuild(), null, null, Collections.singletonMap("onlyRegressions", true));
        assertTrue("The failing test should be reported the first time it fails", content.contains("hudson.plugins.emailext"));

        project.scheduleBuild2(0).get();
        content = failedTestsContent.getContent(project.getLastBuild(), null, null, Collections.singletonMap("onlyRegressions", true));
        assertFalse("The failing test should not be reported the second time it fails", content.contains("hudson.plugins.emailext"));
        assertTrue("The content should state that there are other failing tests still", content.contains("and 1 other failed test"));
    }
}
