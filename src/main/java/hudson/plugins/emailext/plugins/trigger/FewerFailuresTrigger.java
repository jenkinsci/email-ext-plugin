package hudson.plugins.emailext.plugins.trigger;

import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

import java.util.Set;

public class FewerFailuresTrigger extends AbstractFailedTestTrigger {
	@Override
	protected boolean trigger(Set<String> curFailedTests,
			Set<String> prevFailedTests) {
		// The first part of the condition avoids accidental triggering for
		// builds that aggregate downstream test results before those test
		// results are available...
		return curFailedTests.size() > 0 &&
			prevFailedTests.containsAll(curFailedTests) &&
			prevFailedTests.size() > curFailedTests.size();
	}

	@Override
	public EmailTriggerDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	public static final EmailTriggerDescriptor DESCRIPTOR = new EmailTriggerDescriptor() {
		
		@Override
		protected EmailTrigger newInstance() {
			return new FewerFailuresTrigger();
		}
		
		@Override
		public String getTriggerName() {
			return "Fewer failures";
		}
		
		@Override
		public String getHelpText() {
			return "An email will be sent every time a build has strictly fewer failures " +
					"than the previous build (i.e. all current failing tests also failed " +
					"before, but some tests that failed before no longer fail).";
		}
	};
}
