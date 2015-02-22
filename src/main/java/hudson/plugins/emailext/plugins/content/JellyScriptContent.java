package hudson.plugins.emailext.plugins.content;

import hudson.ExtensionList;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.JellyTemplateConfig.JellyTemplateConfigProvider;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.xml.sax.InputSource;

@EmailToken
public class JellyScriptContent extends AbstractEvalContent {

    public static final String MACRO_NAME = "JELLY_SCRIPT";
    private static final String DEFAULT_HTML_TEMPLATE_NAME = "html";
    private static final String DEFAULT_TEXT_TEMPLATE_NAME = "text";
    private static final String DEFAULT_TEMPLATE_NAME = DEFAULT_HTML_TEMPLATE_NAME;
    private static final String JELLY_EXTENSION = ".jelly";
    
    @Parameter
    public String template = DEFAULT_TEMPLATE_NAME;

    public JellyScriptContent() {
        super(MACRO_NAME);
    }
    
    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        InputStream inputStream = null;

        try {
            inputStream = getFileInputStream(template, JELLY_EXTENSION);
            return renderContent(build, inputStream, listener);
        } catch (JellyException e) {
            return "JellyException: " + e.getMessage();
        } catch (FileNotFoundException e) {
            String missingTemplateError = generateMissingFile("Jelly", template);
            return missingTemplateError;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }    

    @Override
    protected ConfigProvider getConfigProvider() {
        if(configProvider == null) {
            ExtensionList<ConfigProvider> providers = ConfigProvider.all();
            configProvider = providers.get(JellyTemplateConfigProvider.class);
        }
        return (ConfigProvider)configProvider;
    }

    private String renderContent(AbstractBuild<?, ?> build, InputStream inputStream, TaskListener listener)
            throws JellyException, IOException {
        JellyContext context = createContext(new ScriptContentBuildWrapper(build), build, listener);
        Script script = context.compileScript(new InputSource(inputStream));
        if (script != null) {
            return convert(build, context, script);
        }
        return null;
    }

    private String convert(AbstractBuild<?, ?> build, JellyContext context, Script script)
            throws JellyTagException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(16 * 1024);
        XMLOutput xmlOutput = XMLOutput.createXMLOutput(output);
        script.run(context, xmlOutput);
        xmlOutput.flush();
        xmlOutput.close();
        output.close();
        return output.toString(getCharset(build));
    }

    private JellyContext createContext(Object it, AbstractBuild<?, ?> build, TaskListener listener) {
        JellyContext context = new JellyContext();
        ExtendedEmailPublisherDescriptor descriptor = Jenkins.getInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        context.setVariable("it", it);
        context.setVariable("build", build);
        context.setVariable("project", build.getParent());
        context.setVariable("logger", listener.getLogger());
        context.setVariable("rooturl", descriptor.getHudsonUrl());
        return context;
    }
}
