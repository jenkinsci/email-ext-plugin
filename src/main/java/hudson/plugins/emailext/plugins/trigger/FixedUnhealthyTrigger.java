package hudson.plugins.emailext.plugins.trigger;

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

public class FixedUnhealthyTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Fixed Unhealthy";

    @DataBoundConstructor
    public FixedUnhealthyTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        super(sendToList, sendToDevs, sendToRequestor, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog, contentType);
    }

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {

        Result buildResult = build.getResult();

        if (buildResult == Result.SUCCESS) {
            AbstractBuild<?, ?> prevBuild = getPreviousBuild(build);
            if (prevBuild != null && (prevBuild.getResult() == Result.UNSTABLE || prevBuild.getResult() == Result.FAILURE)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find most recent previous build matching certain criteria.
     */
    private AbstractBuild<?, ?> getPreviousBuild(AbstractBuild<?, ?> build) {

        AbstractBuild<?, ?> prevBuild = build.getPreviousBuild();

        // Skip ABORTED builds
        if (prevBuild != null && (prevBuild.getResult() == Result.ABORTED)) {
            return getPreviousBuild(prevBuild);
        }

        return prevBuild;
    }

    @Override
    public EmailTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            addTriggerNameToReplace(SuccessTrigger.TRIGGER_NAME);
        }

        @Override
        public String getDisplayName() {
            return TRIGGER_NAME;
        }

        @Override
        public void doHelp(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            rsp.getWriter().println(Messages.FixedTrigger_HelpText());
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
