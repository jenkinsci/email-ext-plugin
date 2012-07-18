package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

public class FirstFailureTrigger extends EmailTrigger {

	public static final String TRIGGER_NAME = "First Failure";
	
	@Override
	public boolean trigger(AbstractBuild<?,?> build) {
		
		Result buildResult = build.getResult();
		
		if (buildResult == Result.FAILURE) {
			AbstractBuild<?,?> prevBuild = build.getPreviousBuild();
            // if there is no previous build, this is a first failure
            // if there is a previous build and it's result was success, this is first failure
			if (prevBuild == null || (prevBuild.getResult() == Result.SUCCESS)) {
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
			addTriggerNameToReplace(SuccessTrigger.TRIGGER_NAME);
		}
		
		@Override
		public String getTriggerName() {
			return TRIGGER_NAME;
		}

		@Override
		public EmailTrigger newInstance() {
			return new FirstFailureTrigger();
		}

		@Override
		public String getHelpText() {
			return "An email will be sent when the build status changes from \"Success\" " +
				   "to \"Failure\"";
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
