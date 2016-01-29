package hudson.plugins.emailext.plugins.content;

import hudson.ExtensionList;
import hudson.model.AbstractBuild;
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
    
    private static Object configProvider;

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

        InputStream inputStream = null;
        String result = "";
        
        try {
            if (!StringUtils.isEmpty(file)) {
                result = IOUtils.toString(getFileInputStream(file, ".txt"));
            } 
        } catch (FileNotFoundException e) {
            String missingFileError = generateMissingFile("Plain Text", file);
            LOGGER.log(Level.SEVERE, missingFileError);
            result = missingFileError;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return result;
    }
    
    @Override
    protected ConfigProvider getConfigProvider() {
        if(configProvider == null) {
            ExtensionList<ConfigProvider> providers = ConfigProvider.all();
            configProvider = providers.get(CustomConfigProvider.class);
        }
        return (ConfigProvider)configProvider;
    }
    
    @Override
    public boolean hasNestedContent() {
        return true;
    }
}
