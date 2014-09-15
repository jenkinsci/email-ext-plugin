package hudson.plugins.emailext.plugins.content;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@EmailToken
public class BuildIdContent extends DataBoundTokenMacro {
    private static final String MACRO_NAME = "BUILD_ID";
    
    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(Run<?, ?> build, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return build.getId();
    }
}
