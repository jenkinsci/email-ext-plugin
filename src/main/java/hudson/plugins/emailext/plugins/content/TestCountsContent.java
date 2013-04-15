package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.EmailToken;
import hudson.tasks.test.AbstractTestResultAction;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Displays the number of tests.
 *
 * @author Seiji Sogabe
 */
@EmailToken
public class TestCountsContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "TEST_COUNTS";
    private static final String VAR_DEFAULT_VALUE = "total";

    @Parameter
    public String var = VAR_DEFAULT_VALUE;
    
    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

 /*
    public String getHelpText() {
        return "Displays the number of tests.\n"
                + "<ul>\n"
                + "<li><i>" + VAR_ARG_NAME + "</i> - Defaults to \"" + VAR_DEFAULT_VALUE + "\".\n"
                + "  <ul>\n"
                + "    <li>total - the number of all tests. </li>\n"
                + "    <li>pass - the number of passed tests. </li>\n"
                + "    <li>fail - the number of failed tests.</li>\n"
                + "    <li>skip - the number of skipped tests.</li> \n"
                + "  </ul>\n"
                + "</li>\n"
                + "</ul>\n";
    }
*/
    
    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        AbstractTestResultAction<?> action = build.getTestResultAction();
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
