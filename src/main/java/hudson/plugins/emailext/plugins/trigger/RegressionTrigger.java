package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

public class RegressionTrigger extends EmailTrigger {
	
	public static final String TRIGGER_NAME = "Regression";
	
	public RegressionTrigger() {
		
	}

	@Override
	public boolean trigger(AbstractBuild<?, ?> build) {
		if (build.getPreviousBuild() == null)
			return build.getResult() == Result.FAILURE;
		if (build.getTestResultAction() == null) return false;
		if (build.getPreviousBuild().getTestResultAction() == null)
			return build.getTestResultAction().getFailCount() > 0;
		return getNumFailures(build) > 
				getNumFailures(build.getPreviousBuild());
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
			return new RegressionTrigger();
		}

		@Override
		public String getHelpText() {
			return Messages.RegressionTrigger_HelpText();
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