package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

public class PreBuildTrigger extends EmailTrigger {
	public static final String TRIGGER_NAME = "Before Build";

	@Override
	public boolean isPreBuild() {
		return true;
	}

	@Override
	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	boolean trigger(B build) {
		return true;
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
			return new PreBuildTrigger();
		}
		
		@Override
		public String getHelpText() {
			return "An email will be sent when the build begins.";
		}
	}
	
	@Override
	public boolean getDefaultSendToDevs() {
		return false;
	}

	@Override
	public boolean getDefaultSendToList() {
		return true;
	}
}
