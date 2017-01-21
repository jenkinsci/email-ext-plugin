package hudson.plugins.emailext.plugins.content;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;

/**
 * Displays the number of tests.
 *
 * @author Seiji Sogabe
 */
@Extension
public class TestCountsContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "TEST_COUNTS";
    private static final String VAR_DEFAULT_VALUE = "total";

    @Parameter
    public String var = VAR_DEFAULT_VALUE;
    
    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }
   
    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build, build.getWorkspace(), listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        AbstractTestResultAction<?> action = run.getAction(AbstractTestResultAction.class);
        if (action == null) {
            return "";
        }

        var = var.toLowerCase();
        
        if ("total".equals(var)) {
            return String.valueOf(action.getTotalCount());
        } else if ("pass".equals(var)) {
            return String.valueOf(action.getTotalCount()-action.getFailCount()-action.getSkipCount());
        } else if ("fail".equals(var)) {
            return String.valueOf(action.getFailCount());
        } else if ("skip".equals(var)) {
            return String.valueOf(action.getSkipCount());
        }

        return "";
    }
}
