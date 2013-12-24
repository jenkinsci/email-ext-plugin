/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.emailext.plugins.trigger;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ScriptSandbox;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public abstract class AbstractScriptTrigger extends EmailTrigger {
    protected String triggerScript;
    
    @DataBoundConstructor
    public AbstractScriptTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, boolean sendToUpstream, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(sendToList, sendToDevs, sendToRequestor, sendToCulprits, sendToUpstream, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
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

        ClassLoader cl = Jenkins.getInstance().getPluginManager().uberClassLoader;
        ScriptSandbox sandbox = null;
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.addCompilationCustomizers(new ImportCustomizer().addStarImports(
                "jenkins",
                "jenkins.model",
                "hudson",
                "hudson.model"));

        ExtendedEmailPublisher publisher = build.getProject().getPublishersList().get(ExtendedEmailPublisher.class);
        if (publisher.getDescriptor().isSecurityEnabled()) {
            cc.addCompilationCustomizers(new SandboxTransformer());
            sandbox = new ScriptSandbox();
        }

        Binding binding = new Binding();
        binding.setVariable("build", build);
        binding.setVariable("project", build.getParent());
        binding.setVariable("rooturl", publisher.getDescriptor().getHudsonUrl());
        binding.setVariable("out", listener.getLogger());
        
        GroovyShell shell = new GroovyShell(cl, binding, cc);

        if (sandbox != null) {
            sandbox.register();
        }

        return shell;
    }

    public abstract static class DescriptorImpl extends EmailTriggerDescriptor {
        @Override
        public boolean getDefaultSendToDevs() {
            return false;
        }

        @Override
        public boolean getDefaultSendToList() {
            return true;
        }
    }
}
