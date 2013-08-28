package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import java.io.IOException;
import javax.servlet.ServletException;
import org.kohsuke.stapler.DataBoundConstructor;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class RegressionTrigger extends EmailTrigger {
    
    public static final String TRIGGER_NAME = "Regression";
    
    @DataBoundConstructor
    public RegressionTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequestor, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }
    
    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        if (build.getPreviousBuild() == null)
            return build.getResult() == Result.FAILURE;
        if (build.getTestResultAction() == null) return false;
        if (build.getPreviousBuild().getTestResultAction() == null)
            return build.getTestResultAction().getFailCount() > 0;

        return build.getTestResultAction().getFailCount() > 
                build.getPreviousBuild().getTestResultAction().getFailCount();
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public EmailTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }
    
    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            addTriggerNameToReplace(UnstableTrigger.TRIGGER_NAME);
            addTriggerNameToReplace(StillUnstableTrigger.TRIGGER_NAME);
        }
        
        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }

        @Override
        public void doHelp(StaplerRequest req, StaplerResponse rsp)throws IOException, ServletException {
            rsp.getWriter().println(Messages.RegressionTrigger_HelpText());
        } 
        
        @Override
        public boolean getDefaultSendToDevs() {
            return true;
        }

        @Override
        public boolean getDefaultSendToList() {
            return true;
        }
    }    
}
