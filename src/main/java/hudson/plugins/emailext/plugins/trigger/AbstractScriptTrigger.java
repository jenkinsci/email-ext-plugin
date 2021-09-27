package hudson.plugins.emailext.plugins.trigger;

import edu.umd.cs.findbugs.annotations.NonNull;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.plugins.emailext.groovy.sandbox.PrintStreamInstanceWhitelist;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

public abstract class AbstractScriptTrigger extends EmailTrigger {

    protected SecureGroovyScript secureTriggerScript;

    public AbstractScriptTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, SecureGroovyScript secureTriggerScript) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
        this.secureTriggerScript = secureTriggerScript;
        StaplerRequest request = Stapler.getCurrentRequest();
        ApprovalContext context = ApprovalContext.create().withCurrentUser();
        if (request != null) {
            context = context.withItem(request.findAncestorObject(Item.class));
        }
        this.secureTriggerScript.configuring(context);
    }

    @Deprecated
    public AbstractScriptTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        this(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType, new SecureGroovyScript(triggerScript, false, null));
    }
    
    @Deprecated
    public AbstractScriptTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
        this.triggerScript = triggerScript;
    }

    @Deprecated
    public String getTriggerScript() {
        return secureTriggerScript.getScript();
    }

    public SecureGroovyScript getSecureTriggerScript() {
        return secureTriggerScript;
    }

    private boolean hasScript() {
        return secureTriggerScript != null && StringUtils.isNotEmpty(secureTriggerScript.getScript());
    }

    @Override
    public boolean configure(@NonNull StaplerRequest req, @NonNull JSONObject formData) {
        super.configure(req, formData);
        if(formData.containsKey("secureTriggerScript")) {
            this.secureTriggerScript = req.bindJSON(SecureGroovyScript.class, formData.getJSONObject("secureTriggerScript"));
            this.secureTriggerScript.configuring(ApprovalContext.create().withCurrentUser().withItem(req.findAncestorObject(Item.class)));
        }
        return true;
    }

    @Override
    public abstract boolean isPreBuild();

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        boolean result = false;
        if (hasScript()) {
            try {
                Object res = evaluate(build, listener);
                if (res != null) {
                    result = (Boolean) res;
                }
            } catch (IOException e) {
                Functions.printStackTrace(e, listener.fatalError("Failed evaluating script trigger %s%n", e.getMessage()));
            }
        }
        return result;
    }
    
    private Object evaluate(AbstractBuild<?, ?> build, TaskListener listener) throws IOException {
        ClassLoader loader = Jenkins.get().getPluginManager().uberClassLoader;
        JenkinsLocationConfiguration configuration = JenkinsLocationConfiguration.get();
        assert configuration != null;

        URLClassLoader urlcl = null;
        List<ClasspathEntry> cp = secureTriggerScript.getClasspath();
        if (!cp.isEmpty()) {
            List<URL> urlList = new ArrayList<>(cp.size());

            for (ClasspathEntry entry : cp) {
                ScriptApproval.get().using(entry);
                urlList.add(entry.getURL());
            }

            loader = urlcl = new URLClassLoader(urlList.toArray(new URL[0]), loader);
        }
        try {
            loader = GroovySandbox.createSecureClassLoader(loader);
            CompilerConfiguration cc;
            if(secureTriggerScript.isSandbox()) {
                cc = GroovySandbox.createSecureCompilerConfiguration();
            } else {
                cc = new CompilerConfiguration();
            }
            cc.addCompilationCustomizers(new ImportCustomizer().addStarImports(
                    "jenkins",
                    "jenkins.model",
                    "hudson",
                    "hudson.model"));

            Binding binding = new Binding();
            binding.setVariable("build", build);
            binding.setVariable("project", build.getParent());
            binding.setVariable("rooturl", configuration.getUrl());
            PrintStream logger = listener.getLogger();
            binding.setVariable("out", logger);

            GroovyShell shell = new GroovyShell(loader, binding, cc);

            if (secureTriggerScript.isSandbox()) {
                try {
                    return GroovySandbox.run(shell, secureTriggerScript.getScript(), new ProxyWhitelist(
                            Whitelist.all(),
                            new PrintStreamInstanceWhitelist(logger)));
                } catch (RejectedAccessException x) {
                    throw ScriptApproval.get().accessRejected(x, ApprovalContext.create());
                }
            } else {
                return shell.evaluate(ScriptApproval.get().using(secureTriggerScript.getScript(), GroovyLanguage.get()));
            }
        } finally {
            if (urlcl != null) {
                urlcl.close();
            }
        }
    }

    @Deprecated
    protected transient String triggerScript;

    /**
     * Called when object has been deserialized from a stream.
     *
     * @return {@code this}, or a replacement for {@code this}.
     * @throws ObjectStreamException if the object cannot be restored.
     * @see <a href="http://download.oracle.com/javase/1.3/docs/guide/serialization/spec/input.doc6.html">The Java Object Serialization Specification</a>
     */
    protected Object readResolve() throws ObjectStreamException {
        if (triggerScript != null && secureTriggerScript == null) {
            this.secureTriggerScript = new SecureGroovyScript(triggerScript, false, null);
            this.secureTriggerScript.configuring(ApprovalContext.create());
            triggerScript = null;
        }
        return this;
    }
}
