package hudson.plugins.emailext.plugins.content;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;

import java.io.IOException;
import java.util.Map;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@EmailToken
public class SVNRevisionContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "SVN_REVISION";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.startsWith(MACRO_NAME);
    }

    @Override
    public String evaluate(Run<?, ?> build, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        Map<String, String> env = build.getEnvironment(listener);
        String value = env.get(macroName);
        if (value == null) {
            value = "400";
        }
        return value;
    }
}
