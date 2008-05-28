package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Project;
import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

public class FixedTrigger extends EmailTrigger {

	public static final String TRIGGER_NAME = "Fixed";
	

	@Override
	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> boolean trigger(
			B build) {
		
		Result buildResult = build.getResult();
		
		if(buildResult == Result.SUCCESS){
			B prevBuild = build.getPreviousBuild();
        	if(prevBuild!=null && (prevBuild.getResult() == Result.UNSTABLE || prevBuild.getResult() == Result.FAILURE))
        		return true;
		}

		return false;
	}

	
	@Override
	public EmailTriggerDescriptor getDescriptor() {
		return DESCRIPTOR;
	}
	
	public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	
	public static final class DescriptorImpl extends EmailTriggerDescriptor{
		
		public DescriptorImpl(){
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
			return "An email will be sent when the build status changes from Failure " +
				   "or Unstable to Successful.";
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
