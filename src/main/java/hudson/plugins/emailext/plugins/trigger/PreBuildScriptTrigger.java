package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;


import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class PreBuildScriptTrigger extends AbstractScriptTrigger {

    public static final String TRIGGER_NAME = "Pre-Build Script";
    
    @DataBoundConstructor
    public PreBuildScriptTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType, String triggerScript) {
        super(sendToList, sendToDevs, sendToRequestor, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType, triggerScript);
    }
    
    @Override
    public boolean isPreBuild() {
        return true;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public EmailTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }

        @Override
        public void doHelp(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            rsp.getWriter().println(Messages.PreBuildScriptTrigger_HelpText());
        }
    }
}
