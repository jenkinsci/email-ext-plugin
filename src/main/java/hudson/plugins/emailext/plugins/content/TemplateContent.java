package hudson.plugins.emailext.plugins.content;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig.CustomConfigProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

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
        String result = "";
        
        try {
            if (!StringUtils.isEmpty(file)) {
                result = IOUtils.toString(getFileInputStream(run, workspace, file, ".txt"), StandardCharsets.UTF_8);
            }
        } catch (FileNotFoundException e) {
            String missingFileError = generateMissingFile("Plain Text", file);
            LOGGER.log(Level.SEVERE, missingFileError, e);
            result = missingFileError;
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
