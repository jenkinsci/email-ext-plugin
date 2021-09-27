package hudson.plugins.emailext.plugins.content;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

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
        
        switch (var) {
            case "total":
                return String.valueOf(action.getTotalCount());
            case "pass":
                return String.valueOf(action.getTotalCount() - action.getFailCount() - action.getSkipCount());
            case "fail":
                return String.valueOf(action.getFailCount());
            case "skip":
                return String.valueOf(action.getSkipCount());
        }

        return "";
    }
}
