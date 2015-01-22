package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.Extension;
import hudson.model.TaskListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension
public class UnevaluatedScriptContent extends DataBoundTokenMacro  {

    public static final String MACRO_NAME = "SCRIPT_CONTENT";
    private static final String DEFAULT_SCRIPT_NAME = "default.groovy";

    @Parameter(required=true)
    public String script = "";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
            return evaluate();
    }
    public String evaluate() throws MacroEvaluationException, IOException, InterruptedException  {
        InputStream inputStream = null;

        try {
            inputStream = getScriptInputStream(script);
            return IOUtils.toString(inputStream, (String)null);
        } catch (IOException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
    
    /**
     * Grabs the requested script from the jenkins instance.
     *
     * @param scriptName
     * @return String
     * @throws java.io.FileNotFoundException
     */
    private InputStream getScriptInputStream(String scriptName)
            throws FileNotFoundException, InterruptedException {
        InputStream inputStream;
        
        final File scriptFile = new File(Jenkins.getInstance().getRootDir(), scriptName);
        inputStream = new FileInputStream(scriptFile);
        
        return inputStream;
    }
}
