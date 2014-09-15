package hudson.plugins.emailext.plugins.content;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@EmailToken
public class ProjectNameContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "PROJECT_NAME";
    public static final String MACRO_NAME2 = "PROJECT_DISPLAY_NAME";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME) || macroName.equals(MACRO_NAME2);
    }

    @Override
    public String evaluate(Run<?, ?> build, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        if(macroName.equals(MACRO_NAME2)) {
            return build.getParent().getDisplayName();
        } 
        return build.getParent().getFullDisplayName();
    }
}
