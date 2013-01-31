package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

public class FixedTrigger extends EmailTrigger {

    public static final String TRIGGER_NAME = "Fixed";

    @Override
    public boolean trigger(AbstractBuild<?, ?> build) {

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
        public String getTriggerName() {
            return TRIGGER_NAME;
        }

        @Override
        public EmailTrigger newInstance() {
            return new FixedTrigger();
        }

        @Override
        public String getHelpText() {
            return Messages.FixedTrigger_HelpText();
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
