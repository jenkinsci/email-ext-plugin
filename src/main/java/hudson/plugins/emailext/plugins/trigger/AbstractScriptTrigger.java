package hudson.plugins.emailext.plugins.trigger;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.kohsuke.stapler.StaplerRequest;

import java.util.List;

public abstract class AbstractScriptTrigger extends EmailTrigger {
    protected String triggerScript;
    
    public AbstractScriptTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
        this.triggerScript = triggerScript;
    }
    
    @Deprecated
    public AbstractScriptTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequester, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(sendToList, sendToDevs, sendToRequester, sendToCulprits,recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
        this.triggerScript = triggerScript;
    }
    
    public String getTriggerScript() {
        return triggerScript;
    }
    
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) {
        super.configure(req, formData);
        if(formData.containsKey("triggerScript")) {
            this.triggerScript = formData.optString("triggerScript", "");
        }
        return true;
    }

    @Override
    public abstract boolean isPreBuild();

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        boolean result = false;
        GroovyShell engine = createEngine(build, listener);
        if (engine != null && !StringUtils.isEmpty(triggerScript)) {
            try {
                Object res = engine.evaluate(triggerScript);
                if (res != null) {
                    result = (Boolean)res;
                } 
            } finally {
                
            }
        }
        return result;
    }
    
    private GroovyShell createEngine(AbstractBuild<?, ?> build, TaskListener listener) {
        ClassLoader cl = Jenkins.getActiveInstance().getPluginManager().uberClassLoader;
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.addCompilationCustomizers(new ImportCustomizer().addStarImports(
                "jenkins",
                "jenkins.model",
                "hudson",
                "hudson.model"));

        Binding binding = new Binding();
        binding.setVariable("build", build);
        binding.setVariable("project", build.getParent());
        binding.setVariable("rooturl", JenkinsLocationConfiguration.get().getUrl());
        binding.setVariable("out", listener.getLogger());

        GroovyShell shell = new GroovyShell(cl, binding, cc);
        return shell;
    }
}
