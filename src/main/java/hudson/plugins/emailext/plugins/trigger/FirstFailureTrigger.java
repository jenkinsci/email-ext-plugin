package hudson.plugins.emailext.plugins.trigger;

import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public class FirstFailureTrigger extends NthFailureTrigger {

	public static final String TRIGGER_NAME = "1st Failure";

	public FirstFailureTrigger() {
		super(1);
	}
	
	@Override
	public EmailTriggerDescriptor getDescriptor() {
		return DESCRIPTOR;
	}
	
	public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final class DescriptorImpl extends NthFailureTrigger.DescriptorImpl {

		@Override
		public String getTriggerName() {
			return TRIGGER_NAME;
		}

		@Override
		public EmailTrigger newInstance(StaplerRequest req, JSONObject formData) {
			return new FirstFailureTrigger();
		}

		@Override
		public String getHelpText() {
			return "An email will be sent when the build status changes from \"Success\" " +
				   "to \"Failure\"";
		}
		
	}
}
