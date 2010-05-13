package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

public class StillUnstableTrigger extends EmailTrigger {

	public static final String TRIGGER_NAME = "Still Unstable";
	
	@Override
	public boolean trigger(AbstractBuild<?,?> build) {
		Result buildResult = build.getResult();
		
		if(buildResult == Result.UNSTABLE) {
			AbstractBuild<?,?> prevBuild = build.getPreviousBuild();
			if (prevBuild != null && (prevBuild.getResult() == Result.UNSTABLE)) {
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
			addTriggerNameToReplace(UnstableTrigger.TRIGGER_NAME);
		}
		
		@Override
		public String getTriggerName() {
			return TRIGGER_NAME;
		}

		@Override
		public EmailTrigger newInstance() {
			return new StillUnstableTrigger();
		}

		@Override
		public String getHelpText() {
			return "An email will be sent if the build status is \"Unstable\" " +
					"for two or more builds in a row.";
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
