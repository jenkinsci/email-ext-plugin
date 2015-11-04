package hudson.plugins.emailext.plugins.content;


import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension
public class ProjectURLContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "PROJECT_URL";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }   

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return "${JENKINS_URL}" + Util.encode(build.getProject().getUrl());
    }

    @Override
    public boolean hasNestedContent() {
        return true;
    }
}
