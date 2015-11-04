package hudson.plugins.emailext.plugins.content;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension
public class ProjectNameContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "PROJECT_NAME";
    public static final String MACRO_NAME2 = "PROJECT_DISPLAY_NAME";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME) || macroName.equals(MACRO_NAME2);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        if(macroName.equals(MACRO_NAME2)) {
            return build.getProject().getDisplayName();
        } 
        return build.getProject().getFullDisplayName();
    }
}
