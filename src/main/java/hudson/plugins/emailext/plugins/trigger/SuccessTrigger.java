package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

public class SuccessTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Success";

    @Override
    public boolean trigger(AbstractBuild<?, ?> build) {
        Result buildResult = build.getResult();

        if (buildResult == Result.SUCCESS) {
            return true;
        }

        return false;
    }

    @Override
    public EmailTriggerDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends EmailTriggerDescriptor {

        @Override
        public String getTriggerName() {
            return TRIGGER_NAME;
        }

        @Override
        public EmailTrigger newInstance() {
            return new SuccessTrigger();
        }

        @Override
        public String getHelpText() {
            return Messages.SuccessTrigger_HelpText();
        }
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
