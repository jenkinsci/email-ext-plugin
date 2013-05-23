/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.emailext.plugins.trigger;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.model.AbstractBuild;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ScriptSandbox;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import static hudson.plugins.emailext.plugins.trigger.ScriptTrigger.TRIGGER_NAME;
import java.io.PrintWriter;
import java.io.StringWriter;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author acearl
 */
public abstract class AbstractScriptTrigger extends EmailTrigger {
    protected String triggerScript;
    
    public String getTriggerScript() {
        return triggerScript;
    }

    @Override
    public abstract boolean isPreBuild();

    @Override
    public boolean trigger(AbstractBuild<?, ?> build) {
        boolean result = false;
        GroovyShell engine = createEngine(build);
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
    
    private GroovyShell createEngine(AbstractBuild<?, ?> build) {

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
        binding.setVariable("build", build);
        binding.setVariable("project", build.getParent());
        binding.setVariable("rooturl", ExtendedEmailPublisher.DESCRIPTOR.getHudsonUrl());

        GroovyShell shell = new GroovyShell(cl, binding, cc);
        StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out);

        if (sandbox != null) {
            sandbox.register();
        }

        return shell;
    }

    @Override
    public abstract EmailTriggerDescriptor getDescriptor();

    public abstract static class DescriptorImpl extends EmailTriggerDescriptor {
        
    }

    @Override
    public boolean getDefaultSendToDevs() {
        return false;
    }

    @Override
    public boolean getDefaultSendToList() {
        return true;
    }    
}
