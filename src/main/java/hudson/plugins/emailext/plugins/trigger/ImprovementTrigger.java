package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public class ImprovementTrigger extends EmailTrigger {
    
    public static final String TRIGGER_NAME = "Improvement";
    
     @Override
    public boolean trigger(AbstractBuild<?, ?> build) {

        if (build.getPreviousBuild() == null)
            return false;
        if (build.getTestResultAction() == null) return false;
        if (build.getPreviousBuild().getTestResultAction() == null)
            return false;
        
        int numCurrFailures = getNumFailures(build);
        
        // The first part of the condition avoids accidental triggering for
        // builds that aggregate downstream test results before those test
        // results are available...
        return build.getTestResultAction().getTotalCount() > 0 &&
            numCurrFailures < getNumFailures(build.getPreviousBuild()) &&
            numCurrFailures > 0;            
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
        public EmailTrigger newInstance(StaplerRequest req, JSONObject formData) {
            return new ImprovementTrigger();
        }

        @Override
        public String getHelpText() {
            return Messages.ImprovementTrigger_HelpText();
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
