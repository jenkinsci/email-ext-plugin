package hudson.plugins.emailext.plugins.trigger;

import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailTrigger;

import java.io.IOException;

import org.junit.Test;

public class SecondFailureTriggerTest extends TriggerTestBase {

	@Override
	EmailTrigger newInstance() {
		return new SecondFailureTrigger();
	}

	@Test
	public void testTrigger_success()
			throws IOException, InterruptedException {
		assertNotTriggered(Result.SUCCESS);
	}

	@Test
	public void testTrigger_firstFailureAfterSuccess()
			throws IOException, InterruptedException {
		assertNotTriggered(Result.SUCCESS, Result.FAILURE);
	}

	@Test
	public void testTrigger_secondFailureAfterSuccess()
			throws IOException, InterruptedException {
		assertTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE);
		assertTriggered(Result.FAILURE, Result.FAILURE, Result.SUCCESS, Result.FAILURE, Result.FAILURE);
	}

	@Test
	public void testTrigger_thirdFailureAfterSuccess()
			throws IOException, InterruptedException {
		assertNotTriggered(Result.SUCCESS, Result.FAILURE, Result.FAILURE, Result.FAILURE);
	}

	@Test
	public void testTrigger_firstBuildFails()
			throws IOException, InterruptedException {
		assertNotTriggered(Result.FAILURE);
	}

	@Test
	public void testTrigger_firstTwoBuildsFail()
			throws IOException, InterruptedException {
		assertTriggered(Result.FAILURE, Result.FAILURE);
	}

	@Test
	public void testTrigger_firstThreeBuildsFail()
			throws IOException, InterruptedException {
		assertNotTriggered(Result.FAILURE, Result.FAILURE, Result.FAILURE);
	}
}

