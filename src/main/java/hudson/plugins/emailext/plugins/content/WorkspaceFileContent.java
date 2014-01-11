package hudson.plugins.emailext.plugins.content;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import java.io.IOException;
import org.codehaus.plexus.util.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;


/**
 * Content token that includes a file in the workspace.
 *
 * @author Kohsuke Kawaguchi
 */
@EmailToken
public class WorkspaceFileContent extends DataBoundTokenMacro  {
    @Parameter(required=true)
    public String path = "";
    @Parameter
    public String fileNotFoundMessage = "ERROR: File '%s' does not exist";
    

    public static final String MACRO_NAME = "FILE";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {        
        // do some environment variable substitution
        try {
            EnvVars env = context.getEnvironment(listener);
            path = env.expand(path);
        } catch(Exception e) {
            listener.error("Error retrieving environment");
        }
        
        if(!context.getWorkspace().child(path).exists()) {
            return String.format(fileNotFoundMessage, path);
        }

        try {
            return context.getWorkspace().child(path).readToString();
        } catch (IOException e) {
            return "ERROR: File '" + path + "' could not be read";
        }
    }

    @Override
    public boolean hasNestedContent() {
        return true;
    }
}
