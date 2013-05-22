package hudson.plugins.emailext.plugins.content;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.text.SimpleTemplateEngine;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ScriptSandbox;
import hudson.plugins.emailext.plugins.EmailToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.groovy.sandbox.SandboxTransformer;

@EmailToken
public class ScriptContent extends DataBoundTokenMacro {

    private static final Logger LOGGER = Logger.getLogger(ScriptContent.class.getName());
    
    @Parameter
    public String script = "";
    
    private static final String DEFAULT_TEMPLATE_NAME = "groovy-html.template";
    
    @Parameter
    public String template = DEFAULT_TEMPLATE_NAME;
    
    private static final String EMAIL_TEMPLATES_DIRECTORY = "email-templates";

    public static final String MACRO_NAME = "SCRIPT";
    
    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    } 

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        InputStream inputStream = null;
        String result = "";
        
        try {
            if (!StringUtils.isEmpty(script)) {
                inputStream = getFileInputStream(script);
                result = executeScript(context, listener, inputStream);
            } else {
                inputStream = getFileInputStream(template);
                result = renderTemplate(context, listener, inputStream);
            }
        } catch (FileNotFoundException e) {
            String missingScriptError = generateMissingFile(script, template);
            LOGGER.log(Level.SEVERE, missingScriptError);
            result = missingScriptError;
        } catch (GroovyRuntimeException e) {
            result = "Error in script or template: " + e.toString();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return result;
    }

    /**
     * Generates a missing file error message
     *
     * @param script name of the script requested
     * @param template name of the template requested
     * @return a message about the missing file
     */
    private String generateMissingFile(String script, String template) {
        if (!StringUtils.isEmpty(script)) {
            return "Script [" + script + "] was not found in $JENKINS_HOME/" + EMAIL_TEMPLATES_DIRECTORY + ".";
        }
        return "Template [" + template + "] was not found in $JENKINS_HOME/" + EMAIL_TEMPLATES_DIRECTORY + ".";
    }

    /**
     * Try to get the script from the classpath first before trying the file
     * system.
     *
     * @param scriptName
     * @return
     * @throws java.io.FileNotFoundException
     */
    private InputStream getFileInputStream(String fileName)
            throws FileNotFoundException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("hudson/plugins/emailext/templates/" + fileName);
        if (inputStream == null) {
            final File scriptsFolder = new File(Hudson.getInstance().getRootDir(), EMAIL_TEMPLATES_DIRECTORY);
            final File scriptFile = new File(scriptsFolder, fileName);
            inputStream = new FileInputStream(scriptFile);
        }
        return inputStream;
    }   
    
    /**
     * Renders the template using a SimpleTemplateEngine
     *
     * @param build the build to act on
     * @param templateStream the template file stream
     * @return the rendered template content
     * @throws IOException
     */
    private String renderTemplate(AbstractBuild<?, ?> build, TaskListener listener, InputStream templateStream)
            throws IOException {
        
        Map<String, Object> binding = new HashMap<String, Object>();
        binding.put("build", build);
        binding.put("listener", listener);
        binding.put("it", new ScriptContentBuildWrapper(build));
        binding.put("rooturl", ExtendedEmailPublisher.DESCRIPTOR.getHudsonUrl());
        binding.put("project", build.getParent());
        
        // we add the binding to the SimpleTemplateEngine instead of the shell
        GroovyShell shell = createEngine(Collections.EMPTY_MAP);
        SimpleTemplateEngine engine = new SimpleTemplateEngine(shell);
        return engine.createTemplate(new InputStreamReader(templateStream)).make(binding).toString();        
    }
    
        /**
     * Executes a script and returns the last value as a String
     *
     * @param build the build to act on
     * @param scriptStream the script input stream
     * @return a String containing the toString of the last item in the script
     * @throws IOException
     */
    private String executeScript(AbstractBuild<?, ?> build, TaskListener listener, InputStream scriptStream)
            throws IOException {
        String result = "";
        Map binding = new HashMap<String, Object>();
        binding.put("build", build);
        binding.put("it", new ScriptContentBuildWrapper(build));
        binding.put("project", build.getParent());
        binding.put("rooturl", ExtendedEmailPublisher.DESCRIPTOR.getHudsonUrl());

        GroovyShell shell = createEngine(binding);
        Object res = shell.evaluate(new InputStreamReader(scriptStream));
        if (res != null) {
            result = res.toString();
        }
        return result;
    }

    /**
     * Creates an engine (GroovyShell) to be used to execute Groovy code
     *
     * @param variables user variables to be added to the Groovy context
     * @return a GroovyShell instance
     * @throws FileNotFoundException
     * @throws IOException
     */
    private GroovyShell createEngine(Map<String, Object> variables)
            throws FileNotFoundException, IOException {

        ClassLoader cl = Jenkins.getInstance().getPluginManager().uberClassLoader;
        ScriptSandbox sandbox = null;
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setScriptBaseClass(EmailExtScript.class.getCanonicalName()); 
        cc.addCompilationCustomizers(new ImportCustomizer().addStarImports(
                "jenkins",
                "jenkins.model",
                "hudson",
                "hudson.model"));

        if (ExtendedEmailPublisher.DESCRIPTOR.isSecurityEnabled()) {
            cc.addCompilationCustomizers(new SandboxTransformer());
            sandbox = new ScriptSandbox();
        }

        Binding binding = new Binding();
        for (Map.Entry<String, Object> e : variables.entrySet()) {
            binding.setVariable(e.getKey(), e.getValue());
        }

        GroovyShell shell = new GroovyShell(cl, binding, cc);
        if (sandbox != null) {
            sandbox.register();
        }
        return shell;
    }

    @Override
    public boolean hasNestedContent() {
        return false;
    }
}
