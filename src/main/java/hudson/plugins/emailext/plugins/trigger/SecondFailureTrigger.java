package hudson.plugins.emailext.plugins.trigger;

import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

public class SecondFailureTrigger extends NthFailureTrigger {

	public static final String TRIGGER_NAME = "2nd Failure";

	public SecondFailureTrigger() {
		super(2);
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
		public EmailTrigger newInstance() {
			return new SecondFailureTrigger();
		}

		@Override
		public String getHelpText() {
			return "An email will be sent when the build fails twice in a row after a successful build.";
		}
	}
}
