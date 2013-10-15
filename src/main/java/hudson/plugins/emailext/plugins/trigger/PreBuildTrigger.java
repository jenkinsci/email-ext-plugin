package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class PreBuildTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Before Build";
    
    @DataBoundConstructor
    public PreBuildTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequestor, sendToCulprits, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean isPreBuild() {
        return true;
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }

        @Override
        public void doHelp(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            rsp.getWriter().println(Messages.PreBuildTrigger_HelpText());
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
