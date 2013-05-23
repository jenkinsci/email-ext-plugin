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
import hudson.plugins.emailext.plugins.content.ScriptContentBuildWrapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author acearl
 */
public class PreBuildScriptTrigger extends AbstractScriptTrigger {

    public static final String TRIGGER_NAME = "Pre-Build Script Trigger";
    
    @Override
    public boolean isPreBuild() {
        return true;
    }

    @Override
    public EmailTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractScriptTrigger.DescriptorImpl {

        @Override
        public String getTriggerName() {
            return TRIGGER_NAME;
        }

        @Override
        public EmailTrigger newInstance(StaplerRequest req, JSONObject formData) {
            PreBuildScriptTrigger trigger = new PreBuildScriptTrigger();
            if(formData != null) {
              trigger.triggerScript = formData.getString("email_ext_prebuildscripttrigger_script");
            }
            return trigger;
        }

        @Override
        public String getHelpText() {
            return Messages.PreBuildScriptTrigger_HelpText();
        }
    }
}
