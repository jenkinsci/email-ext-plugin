package hudson.plugins.emailext.plugins.content;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig.CustomConfigProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

@EmailToken
public class TemplateContent extends AbstractEvalContent {

    private static final Logger LOGGER = Logger.getLogger(TemplateContent.class.getName());
    
    @Parameter(required=true)
    public String file = "";
    
    public static final String MACRO_NAME = "TEMPLATE";
    
    public TemplateContent() {
        super(MACRO_NAME);
    }
    
    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build, build.getWorkspace(), listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        InputStream inputStream = null;
        String result = "";
        
        try {
            if (!StringUtils.isEmpty(file)) {
                result = IOUtils.toString(getFileInputStream(workspace, file, ".txt"));
            }
        } catch (FileNotFoundException e) {
            String missingFileError = generateMissingFile("Plain Text", file);
            LOGGER.log(Level.SEVERE, missingFileError, e);
            result = missingFileError;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return result;
    }

    protected Class<? extends ConfigProvider> getProviderClass() {
        return CustomConfigProvider.class;
    }
    
    @Override
    public boolean hasNestedContent() {
        return true;
    }
}
