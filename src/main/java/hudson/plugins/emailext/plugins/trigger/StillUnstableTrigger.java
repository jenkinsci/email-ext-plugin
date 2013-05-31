package hudson.plugins.emailext.plugins.trigger;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import java.io.IOException;
import javax.servlet.ServletException;
import org.kohsuke.stapler.DataBoundConstructor;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class StillUnstableTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Still Unstable";
    
    @DataBoundConstructor
    public StillUnstableTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog) {
        super(sendToList, sendToDevs, sendToRequestor, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        Result buildResult = build.getResult();

        if (buildResult == Result.UNSTABLE) {
            AbstractBuild<?, ?> prevBuild = build.getPreviousBuild();
            if (prevBuild != null && (prevBuild.getResult() == Result.UNSTABLE)) {
                return true;
            }
        }

        return false;
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
        }

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }

        @Override
        public void doHelp(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            rsp.getWriter().println(Messages.StillUnstableTrigger_HelpText());
        }
        
        @Override
        public boolean getDefaultSendToDevs() {
            return true;
        }

        @Override
        public boolean getDefaultSendToList() {
            return false;
        }
    }    
}
