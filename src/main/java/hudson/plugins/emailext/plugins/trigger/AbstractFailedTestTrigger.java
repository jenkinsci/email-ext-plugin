package hudson.plugins.emailext.plugins.trigger;

import hudson.model.AbstractBuild;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.tasks.test.TestResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractFailedTestTrigger extends EmailTrigger {
	
	@Override
	public boolean trigger(AbstractBuild<?, ?> build) {
		if (build.getPreviousBuild() == null)
			return false;
		if (build.getTestResultAction() == null) return false;
		if (build.getPreviousBuild().getTestResultAction() == null)
			return false;
		
		Set<String> failedTests = new LinkedHashSet<String>();
		Set<String> prevFailedTests = new LinkedHashSet<String>();
		for (Object r : build.getTestResultAction().getFailedTests())
			failedTests.add(((TestResult)r).getName());
		for (Object r : build.getPreviousBuild().getTestResultAction().getFailedTests())
			prevFailedTests.add(((TestResult)r).getName());
		
		return trigger(failedTests, prevFailedTests);
	}
	
	protected abstract boolean trigger(Set<String> curFailedTests, Set<String> prevFailedTests);

	public abstract EmailTriggerDescriptor getDescriptor();
	
	@Override
	public boolean getDefaultSendToDevs() {
		return true;
	}

	@Override
	public boolean getDefaultSendToList() {
		return true;
	}
}
