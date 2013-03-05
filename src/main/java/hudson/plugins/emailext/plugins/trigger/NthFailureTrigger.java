package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;

/**
 * Triggers an email after the specified number of consecutive failures (preceeded by a successful
 * build).
 */
public abstract class NthFailureTrigger extends EmailTrigger {

	private final int failureCount;

	public NthFailureTrigger(int failureCount) {
		this.failureCount = failureCount;
	}

	@Override
	public boolean trigger(AbstractBuild<?, ?> build) {

		// Work back through the failed builds.
		for (int i = 0; i < failureCount; i++) {
			if (build == null) {
				// We don't have enough history to have reached the failure count.
				return false;
			}

			Result buildResult = build.getResult();
			if (buildResult != Result.FAILURE) {
				return false;
			}

			build = build.getPreviousBuild();
		}

		// Check the the preceding build was a success.
		// if there is no previous build, this is a first failure
		// if there is a previous build and it's result was success, this is first failure
		if (build == null || build.getResult() == Result.SUCCESS) {
			return true;
		}

		return false;
	}

	@Override
	public boolean getDefaultSendToDevs() {
		return true;
	}

	@Override
	public boolean getDefaultSendToList() {
		return true;
	}

	public abstract static class DescriptorImpl extends EmailTriggerDescriptor {

		public DescriptorImpl() {
			addTriggerNameToReplace(FailureTrigger.TRIGGER_NAME);
			addTriggerNameToReplace(StillFailingTrigger.TRIGGER_NAME);
		}
	}
}
