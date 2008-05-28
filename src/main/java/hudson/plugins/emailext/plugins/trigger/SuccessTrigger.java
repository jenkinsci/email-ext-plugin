package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Project;
import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

public class SuccessTrigger extends EmailTrigger {
	public static final String TRIGGER_NAME = "Success";

	@Override
	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> boolean trigger(
			B build) {
		Result buildResult = build.getResult();
		
		if(buildResult == Result.SUCCESS)
			return true;
		
		return false;
	}
	

	@Override
	public EmailTriggerDescriptor getDescriptor() {
		return DESCRIPTOR;
	}
	
	public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	
	public static final class DescriptorImpl extends EmailTriggerDescriptor{
				
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
			return "An email will be sent if the build status is \"Successful\". "+
					"If the \"Fixed\" trigger is configured, and the previous build " +
					"status was \"Failure\" or \"Unstable\", then a the \"Fixed\" trigger " +
					"will send an email instead.";
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
