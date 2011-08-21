package hudson.plugins.emailext.plugins.trigger;

import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import java.util.Set;

public class ImprovementTrigger extends AbstractFailedTestTrigger {
	@Override
	protected boolean trigger(Set<String> curFailedTests,
			Set<String> prevFailedTests) {
		return curFailedTests.size() > 0 &&
			curFailedTests.size() < prevFailedTests.size();
	}

	@Override
	public EmailTriggerDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	public static final EmailTriggerDescriptor DESCRIPTOR = new EmailTriggerDescriptor() {
		
		@Override
		protected EmailTrigger newInstance() {
			return new ImprovementTrigger();
		}
		
		@Override
		public String getTriggerName() {
			return "Test improvement";
		}
		
		@Override
		public String getHelpText() {
			return "An email will be sent every time a build fewer failing tests than " +
					"the previous build (regardless of whether some tests were broken), " +
					"but still has at least one failing test.";
		}
	};
}
