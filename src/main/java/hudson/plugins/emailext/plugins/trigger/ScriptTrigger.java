package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import java.io.IOException;
import javax.servlet.ServletException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ScriptTrigger extends AbstractScriptTrigger {

    public static final String TRIGGER_NAME = "Script";
    
    @DataBoundConstructor
    public ScriptTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(sendToList, sendToDevs, sendToRequestor, sendToCulprits, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType, triggerScript);
    }

    @Override
    public boolean isPreBuild() {
        return false;
    }

    @Override
    public EmailTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends EmailTriggerDescriptor {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }

        @Override
        public void doHelp(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            rsp.getWriter().println(Messages.ScriptTrigger_HelpText());
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
}
