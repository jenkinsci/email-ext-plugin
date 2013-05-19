package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public class StillFailingTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Still Failing";

    @Override
    public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
        Result buildResult = build.getResult();

        if (buildResult == Result.FAILURE) {
            AbstractBuild<?, ?> prevBuild = build.getPreviousBuild();
            if (prevBuild != null && (prevBuild.getResult() == Result.FAILURE)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public EmailTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        public DescriptorImpl() {
            //This trigger should send an email in place of the Failure Trigger
            addTriggerNameToReplace(FailureTrigger.TRIGGER_NAME);
        }

        @Override
        public String getTriggerName() {
            return TRIGGER_NAME;
        }

        @Override
        public EmailTrigger newInstance(StaplerRequest req, JSONObject formData) {
            return new StillFailingTrigger();
        }

        @Override
        public String getHelpText() {
            return Messages.StillFailingTrigger_HelpText();
        }
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
