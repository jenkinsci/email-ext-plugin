package hudson.plugins.emailext.plugins.trigger;

import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import java.util.Set;

public class RegressionTrigger extends AbstractFailedTestTrigger {
	@Override
	protected boolean trigger(Set<String> curFailedTests,
			Set<String> prevFailedTests) {
		return curFailedTests.size() > prevFailedTests.size();
	}

	@Override
	public EmailTriggerDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	public static final EmailTriggerDescriptor DESCRIPTOR = new EmailTriggerDescriptor() {
		
		@Override
		protected EmailTrigger newInstance() {
			return new RegressionTrigger();
		}
		
		@Override
		public String getTriggerName() {
			return "More failures";
		}
		
		@Override
		public String getHelpText() {
			return "An email will be sent every time a build has more failing tests than " +
					"the previous build (regardless of whether some tests were fixed).";
		}
	};
}
