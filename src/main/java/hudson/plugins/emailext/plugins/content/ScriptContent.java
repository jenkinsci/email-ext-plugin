package hudson.plugins.emailext.plugins.content;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.GroovyTemplateConfig.GroovyTemplateConfigProvider;
import hudson.plugins.emailext.groovy.sandbox.EmailExtScriptTokenMacroWhitelist;
import hudson.plugins.emailext.groovy.sandbox.PrintStreamInstanceWhitelist;
import hudson.plugins.emailext.groovy.sandbox.StaticProxyInstanceWhitelist;
import hudson.plugins.emailext.groovy.sandbox.TaskListenerInstanceWhitelist;
import hudson.plugins.emailext.plugins.EmailToken;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
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
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@EmailToken
public class ScriptContent extends AbstractEvalContent {

    private static final Logger LOGGER = Logger.getLogger(ScriptContent.class.getName());
    
    @Parameter
    public String script = "";
    
    private static final String DEFAULT_TEMPLATE_NAME = "groovy-html.template";
    
    @Parameter
    public String template = DEFAULT_TEMPLATE_NAME;
    
    public static final String MACRO_NAME = "SCRIPT";
    
    private static final Map<String,Reference<Template>> templateCache = new HashMap<>();
    
    public ScriptContent() {
        super(MACRO_NAME);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        InputStream inputStream = null;
        String result = "";
        
        try {
            if (!StringUtils.isEmpty(script)) {
                inputStream = getFileInputStream(run, workspace, script, ".groovy");
                result = executeScript(run, workspace, listener, inputStream);
            } else {
                inputStream = getFileInputStream(run, workspace, template, ".template");
                result = renderTemplate(run, workspace, listener, inputStream);
            }
        } catch (FileNotFoundException e) {
            String missingScriptError = "";
            if (!StringUtils.isEmpty(script)) {
                missingScriptError = generateMissingFile("Groovy Script", script);
            } else {
                missingScriptError = generateMissingFile("Groovy Template", template);
            }
            LOGGER.log(Level.SEVERE, missingScriptError);
            result = missingScriptError;
        } catch (GroovyRuntimeException e) {
            result = "Error in script or template: " + e;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return result;
    }

    @Override
    protected Class<? extends ConfigProvider> getProviderClass () {
        return GroovyTemplateConfigProvider.class;
    }
    
    /**
     * Renders the template using a SimpleTemplateEngine
     *
     * @param build the build to act on
     * @param templateStream the template file stream
     * @return the rendered template content
     */
    private String renderTemplate(Run<?, ?> build, FilePath workspace, TaskListener listener, InputStream templateStream) {
        
        String result;
        
        final Map<String, Object> binding = new HashMap<>();
        ExtendedEmailPublisherDescriptor descriptor = Jenkins.get().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        binding.put("build", build);
        binding.put("listener", listener);
        binding.put("it", new ScriptContentBuildWrapper(build));
        binding.put("rooturl", descriptor.getHudsonUrl());
        binding.put("project", build.getParent());
        binding.put("workspace", workspace);

        try {
            String text = IOUtils.toString(templateStream, StandardCharsets.UTF_8);
            boolean approvedScript = false;
            if (templateStream instanceof UserProvidedContentInputStream && !AbstractEvalContent.isApprovedScript(text, GroovyLanguage.get())) {
                approvedScript = false;
                ScriptApproval.get().configuring(text, GroovyLanguage.get(), ApprovalContext.create().withItem(build.getParent()));
            } else {
                approvedScript = true;
            }
            // we add the binding to the SimpleTemplateEngine instead of the shell
            GroovyShell shell = createEngine(descriptor, Collections.emptyMap(), !approvedScript);
            SimpleTemplateEngine engine = new SimpleTemplateEngine(shell);
            Template tmpl;
            synchronized (templateCache) {
                Reference<Template> templateR = templateCache.get(text);
                tmpl = templateR == null ? null : templateR.get();
                if (tmpl == null) {
                    tmpl = engine.createTemplate(text);
                    templateCache.put(text, new SoftReference<>(tmpl));
                }
            }
            final Template tmplR = tmpl;
            if (approvedScript) {
                //The script has been approved by an admin, so run it as is
                result = tmplR.make(binding).toString();
            } else {
                //unapproved script, so run in sandbox
                StaticProxyInstanceWhitelist whitelist = new StaticProxyInstanceWhitelist(build, "templates-instances.whitelist");
                result = GroovySandbox.runInSandbox(() -> {
                    return tmplR.make(binding).toString(); //TODO there is a PrintWriter instance created in make and bound to out
                }, new ProxyWhitelist(
                        Whitelist.all(),
                        new TaskListenerInstanceWhitelist(listener),
                        new PrintStreamInstanceWhitelist(listener.getLogger()),
                        new EmailExtScriptTokenMacroWhitelist(),
                        whitelist));
            }

        } catch(Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            Functions.printStackTrace(e, pw);
            result = "Exception raised during template rendering: " + e.getMessage() + "\n\n" + sw;
        }
        return result;
    }

    /**
     * Executes a script and returns the last value as a String
     *
     * @param build        the build to act on
     * @param scriptStream the script input stream
     * @return a String containing the toString of the last item in the script
     */
    private String executeScript(Run<?, ?> build, FilePath workspace, TaskListener listener, InputStream scriptStream)
            throws IOException {
        String result = "";
        Map<String, Object> binding = new HashMap<>();
        ExtendedEmailPublisherDescriptor descriptor = Jenkins.get().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        Item parent = build.getParent();

        binding.put("build", build);
        binding.put("it", new ScriptContentBuildWrapper(build));
        binding.put("project", parent);
        binding.put("rooturl", descriptor.getHudsonUrl());
        binding.put("workspace", workspace);
        PrintStream logger = listener.getLogger();
        binding.put("logger", logger);

        String scriptContent = IOUtils.toString(scriptStream, descriptor.getCharset());

        if (scriptStream instanceof UserProvidedContentInputStream) {
            ScriptApproval.get().configuring(scriptContent, GroovyLanguage.get(), ApprovalContext.create().withItem(parent));
        }

        if (scriptStream instanceof UserProvidedContentInputStream && !AbstractEvalContent.isApprovedScript(scriptContent, GroovyLanguage.get())) {
            //Unapproved script, run it in the sandbox
            GroovyShell shell = createEngine(descriptor, binding, true);
            Object res = GroovySandbox.run(shell, scriptContent, new ProxyWhitelist(
                    Whitelist.all(),
                    new PrintStreamInstanceWhitelist(logger),
                    new EmailExtScriptTokenMacroWhitelist()

            ));
            if (res != null) {
                result = res.toString();
            }
        } else {
            if (scriptStream instanceof UserProvidedContentInputStream) {
                ScriptApproval.get().using(scriptContent, GroovyLanguage.get());
            }
            //Pre approved script, so run as is
            GroovyShell shell = createEngine(descriptor, binding, false);
            Script script = shell.parse(scriptContent);
            Object res = script.run();
            if (res != null) {
                result = res.toString();
            }
        }

        return result;
    }

    /**
     * Creates an engine (GroovyShell) to be used to execute Groovy code
     *
     * @param variables user variables to be added to the Groovy context
     * @return a GroovyShell instance
     */
    private GroovyShell createEngine(ExtendedEmailPublisherDescriptor descriptor, Map<String, Object> variables, boolean secure) {

        ClassLoader cl;
        CompilerConfiguration cc;
        if (secure) {
            cl = GroovySandbox.createSecureClassLoader(Jenkins.get().getPluginManager().uberClassLoader);
            cc = GroovySandbox.createSecureCompilerConfiguration();
        } else {
            cl = Jenkins.get().getPluginManager().uberClassLoader;
            cc = new CompilerConfiguration();
        }
        cc.setScriptBaseClass(EmailExtScript.class.getCanonicalName()); 
        cc.addCompilationCustomizers(new ImportCustomizer().addStarImports(
                "jenkins",
                "jenkins.model",
                "hudson",
                "hudson.model"));

        Binding binding = new Binding();
        for (Map.Entry<String, Object> e : variables.entrySet()) {
            binding.setVariable(e.getKey(), e.getValue());
        }

        return new GroovyShell(cl, binding, cc);
    }

    @Override
    public boolean hasNestedContent() {
        return false;
    }
}
