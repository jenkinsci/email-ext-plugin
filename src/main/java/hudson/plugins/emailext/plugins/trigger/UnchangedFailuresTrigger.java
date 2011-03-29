package hudson.plugins.emailext.plugins.trigger;

import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import java.util.Set;

public class UnchangedFailuresTrigger extends AbstractFailedTestTrigger {
	@Override
	protected boolean trigger(Set<String> curFailedTests,
			Set<String> prevFailedTests) {
		return curFailedTests.size() > 0 &&
			curFailedTests.equals(prevFailedTests);
	}

	@Override
	public EmailTriggerDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	public static final EmailTriggerDescriptor DESCRIPTOR = new EmailTriggerDescriptor() {
		
		@Override
		protected EmailTrigger newInstance() {
			return new UnchangedFailuresTrigger();
		}
		
		@Override
		public String getTriggerName() {
			return "Stable failures";
		}
		
		@Override
		public String getHelpText() {
			return "An email will be sent every time a build has precisely the same set " +
					"of failing tests as the previous build; no email will be sent for " +
					"stable builds.";
		}
	};
}
