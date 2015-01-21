package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.content.UnevaluatedScriptContent;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class UnevaluatedScriptContentTest {

    @Rule
    public JenkinsRule j = new JenkinsRule(){
        @Override
        public void before() throws Throwable {
            super.before();

            AbstractProject project = mock(AbstractProject.class);

            build = mock(AbstractBuild.class);
            listener = StreamTaskListener.fromStdout();

            when(build.getProject()).thenReturn(project);
        }
    };

    private AbstractBuild build;
    private TaskListener listener;
    private int scriptID = 0;

    @Test
    public void testIsProperlyEvaluated() throws Exception {
        String scriptContent = "println \"Hello World!\"";
        String scriptName    = makeScript(scriptContent);

        String macroEvaluated = TokenMacro.expand(build, listener, generateToken(scriptName));

        assertThat("Expanding macro produces expected outcome", 
            scriptContent, equalTo(macroEvaluated)
        );
    }

    @Test
    public void testNoSubEvaluation() throws Exception {
        String scriptContent = "BUILD_URL = \"World\"\n" 
                             + "println \"Hello $BUILD_URL\"\n";
        String scriptName    = makeScript(scriptContent);

        String macroEvaluated = TokenMacro.expand(build, listener, generateToken(scriptName));

        assertThat("Script content is not expanded internally", 
            scriptContent, equalTo(macroEvaluated)
        );
    }

    private String makeScript(String content) throws IOException {
        String name = "UnevaluatedScriptContent_script" + scriptID + ".groovy";
        scriptID++;
        File f = new File(j.jenkins.getRootDir(), name);
        PrintStream out = new PrintStream(f);
        out.print(content);
        out.flush();
        out.close();
        return name;
    }

    private String generateToken(String scriptName) {
        return "${" 
                + UnevaluatedScriptContent.MACRO_NAME 
                + ",script=\"" 
                + scriptName 
                + "\"}"; 
    }

}
