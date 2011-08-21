package hudson.plugins.emailext.plugins.trigger;

import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import java.util.Set;

public class ChangedFailuresTrigger extends AbstractFailedTestTrigger {
	@Override
	protected boolean trigger(Set<String> curFailedTests,
			Set<String> prevFailedTests) {
		return !prevFailedTests.containsAll(curFailedTests) &&
			!curFailedTests.containsAll(prevFailedTests);
	}

	@Override
	public EmailTriggerDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	public static final EmailTriggerDescriptor DESCRIPTOR = new EmailTriggerDescriptor() {
		
		@Override
		protected EmailTrigger newInstance() {
			return new ChangedFailuresTrigger();
		}
		
		@Override
		public String getTriggerName() {
			return "Changed failures";
		}
		
		@Override
		public String getHelpText() {
			return "An email will be sent every time a build has different failures than " +
					"the previous build, meaning that there are some tests that were fixed " +
					"and some tests that were broken.";
		}
	};
}
