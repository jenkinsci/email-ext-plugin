package hudson.plugins.emailext.plugins.content;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ScriptSandbox;
import hudson.plugins.emailext.plugins.ContentBuilder;
import hudson.plugins.emailext.plugins.EmailContent;

import jenkins.model.Jenkins;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.runtime.MethodClosure;
import org.kohsuke.groovy.sandbox.SandboxTransformer;

public class ScriptContent implements EmailContent {

    private static final Logger LOGGER = Logger.getLogger(ScriptContent.class.getName());
    public static final String SCRIPT_NAME_ARG = "script";
    public static final String SCRIPT_TEMPLATE_ARG = "template";
    private static final String DEFAULT_SCRIPT_NAME = "email-ext.groovy";
    private static final String DEFAULT_TEMPLATE_NAME = "groovy-html.template";
    private static final String EMAIL_TEMPLATES_DIRECTORY = "email-templates";

    public String getToken() {
        return "SCRIPT";
    }

    public String getHelpText() {
        StringBuilder helpText = new StringBuilder("Custom message content generated from a groovy script. "
                + "Custom scripts should be placed in "
                + "$JENKINS_HOME/" + EMAIL_TEMPLATES_DIRECTORY + ". When using custom scripts, "
                + "the script filename should be used for "
                + "the \"" + SCRIPT_NAME_ARG + "\" argument.\n"
                + "templates and other items may be loaded using the\n"
                + "host.readFile(String fileName) function\n"
                + "the function will look in the resources for\n"
                + "the email-ext plugin first, and then in the $JENKINS_HOME/" + EMAIL_TEMPLATES_DIRECTORY + "\n"
                + "directory. No other directories will be searched.\n"
                + "<ul>\n"
                + "<li><i>" + SCRIPT_NAME_ARG + "</i> - the script name.<br>\n"
                + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Defaults to \"" + DEFAULT_SCRIPT_NAME + "\".</li>\n"
                + "<li><i>" + SCRIPT_TEMPLATE_ARG + "</i> - the template filename.<br>\n"
                + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Defaults to \"" + DEFAULT_TEMPLATE_NAME + "\"</li>\n"
                + "</ul>\n");
        return helpText.toString();
    }

    public List<String> getArguments() {
        List<String> args = new ArrayList<String>();
        args.add(SCRIPT_NAME_ARG);
        args.add(SCRIPT_TEMPLATE_ARG);
        return args;
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher, EmailType type, Map<String, ?> args)
            throws IOException, InterruptedException {

        InputStream inputStream = null;
        String result = "";
        String scriptName = Args.get(args, SCRIPT_NAME_ARG, "");
        String templateName = Args.get(args, SCRIPT_TEMPLATE_ARG, DEFAULT_TEMPLATE_NAME);

        try {
            if (!StringUtils.isEmpty(scriptName)) {
                inputStream = getFileInputStream(scriptName);
                result = executeScript(build, inputStream);
            } else {
                inputStream = getFileInputStream(templateName);
                result = renderTemplate(build, inputStream);
            }
        } catch (FileNotFoundException e) {
            String missingScriptError = generateMissingFile(scriptName, templateName);
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
     * Closure class so that content tokens will work as functions
     * in the templates.
     */
    private class ContentClosure extends Closure {
        private EmailContent content;
        private AbstractBuild<?, ?> build;
        
        public ContentClosure(EmailContent content, AbstractBuild<?,?> build) {
            super(content);
            this.content = content;
            this.build = build;
        }
        
        /**
         * This method will be called from Groovy when a content token is used.
         * @param params the arguments for the content token.
         * @return the rendered content token
         */
        public Object doCall(Object params) {            
            String result;
            try {
                Map args = (Map)params;
                result = content.getContent(build, null, null, args);
            } catch(Exception e) {
                result = "[Error processing token: " + content.getToken() + "]";
            }
            return result;
        }
    }

    /**
     * Renders the template using a SimpleTemplateEngine
     *
     * @param build the build to act on
     * @param templateStream the template file stream
     * @return the rendered template content
     * @throws IOException
     */
    private String renderTemplate(AbstractBuild<?, ?> build, InputStream templateStream)
            throws IOException {
        Map binding = new HashMap<String, Object>();

        binding.put("build", build);
        binding.put("it", new ScriptContentBuildWrapper(build));
        binding.put("project", build.getParent());
        binding.put("rooturl", ExtendedEmailPublisher.DESCRIPTOR.getHudsonUrl());
        
        // add the content tokens as closures
        for(EmailContent content : ContentBuilder.getEmailContentTypes()) {
            binding.put(content.getToken(), new ContentClosure(content, build));
        }
        
        // we add the binding to the SimpleTemplateEngine instead of the shell
        GroovyShell shell = createEngine(Collections.EMPTY_MAP);
        SimpleTemplateEngine engine = new SimpleTemplateEngine(shell);

        Writable w = engine.createTemplate(new InputStreamReader(templateStream)).make(binding);
        StringWriter content = new StringWriter();
        w.writeTo(content);

        return content.toString();
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

    /**
     * Executes a script and returns the last value as a String
     *
     * @param build the build to act on
     * @param scriptStream the script input stream
     * @return a String containing the toString of the last item in the script
     * @throws IOException
     */
    private String executeScript(AbstractBuild<?, ?> build, InputStream scriptStream)
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

    public boolean hasNestedContent() {
        return false;
    }
}
