package hudson.plugins.emailext.plugins.content;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.EnvironmentVariablesNodeProperty;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.Bug;

import java.util.Collections;

public class EnvContentTest extends HudsonTestCase {
	@Bug(5465)
	public void testGetContent_shouldGetEnvironmentVariablesNodeProperties() throws Exception {
		hudson.getGlobalNodeProperties().add(new EnvironmentVariablesNodeProperty(
				new EnvironmentVariablesNodeProperty.Entry("GLOBAL", "global-property"),
				new EnvironmentVariablesNodeProperty.Entry("NODE_SHOULD_OVERRIDE", "node-property-should-be-overridden")));
		hudson.getNodeProperties().add(new EnvironmentVariablesNodeProperty(
				new EnvironmentVariablesNodeProperty.Entry("NODE_SHOULD_OVERRIDE", "node-property")));

		FreeStyleProject project = createFreeStyleProject();
		FreeStyleBuild build = project.scheduleBuild2(0).get();

		EnvContent content = new EnvContent();

		String global = content.getContent(build, null, null, Collections.singletonMap("var", "GLOBAL"));
		assertEquals("global-property", global);
		String node = content.getContent(build, null, null, Collections.singletonMap("var", "NODE_SHOULD_OVERRIDE"));
		assertEquals("node-property", node);
	}
}
