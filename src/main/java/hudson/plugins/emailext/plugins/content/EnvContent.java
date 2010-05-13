package hudson.plugins.emailext.plugins.content;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.StreamBuildListener;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.slaves.NodeProperty;
import hudson.tasks.BuildWrapper;
import hudson.util.NullStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An EmailContent for build log. Shows last 250 lines of the build log file.
 * 
 * @author jjamison
 */
public class EnvContent implements EmailContent {
	
	private static final String TOKEN = "ENV";
	
	private static final String VAR_ARG_NAME = "var";
	private static final String VAR_DEFAULT_VALUE = "";
	
	public String getToken() {
		return TOKEN;
	}

	public List<String> getArguments() {
		return Collections.singletonList(VAR_ARG_NAME);
	}
	
	public String getHelpText() {
		return "Displays an environment variable.\n" +
		"<ul>\n" +
		
		"<li><i>" + VAR_ARG_NAME + "</i> - the name of the environment " +
				"variable to display.  If \"\", show all.<br>\n" +
		"Defaults to \"" + VAR_DEFAULT_VALUE + "\".\n" +
		
		"</ul>\n";
	}

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
			EmailType emailType, Map<String, ?> args) throws IOException, InterruptedException {
		String var = Args.get(args, VAR_ARG_NAME, VAR_DEFAULT_VALUE);

		// The following was copied from the batch-task plugin.
		// Copying some logic from AbstractBuild.AbstractRunner.createLauncher().
		// buildEnvironments are discarded after the build runs, so we need to follow the
		// same model here.. applying node properties, but leaving out build wrappers.
		final ArrayList<Environment> buildEnvironments = new ArrayList<Environment>();
		Node node = build.getBuiltOn();
		if (node == null) {
			// Fallback to master.
			node = Hudson.getInstance();
		}
		BuildListener listener = new StreamBuildListener(new NullStream());
		Launcher launcher = node.createLauncher(listener);
		try {
			for (NodeProperty<?> nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {
				Environment environment = nodeProperty.setUp(build, launcher, listener);
				if (environment != null) {
					buildEnvironments.add(environment);
				}
			}

			for (NodeProperty<?> nodeProperty : node.getNodeProperties()) {
				Environment environment = nodeProperty.setUp(build, launcher, listener);
				if (environment != null) {
					buildEnvironments.add(environment);
				}
			}
			// Not sure if email-ext should use all build wrappers (xvnc for example),
			// but look for one in particular, from setenv plugin.
			if (build.getProject() instanceof BuildableItemWithBuildWrappers) {
				for (BuildWrapper wrapper : ((BuildableItemWithBuildWrappers)build.getProject()).getBuildWrappersList()) {
					if ("hudson.plugins.setenv.SetEnvBuildWrapper".equals(wrapper.getClass().getName())) {
						Environment environment = wrapper.setUp(build, launcher, listener);
						if (environment != null) {
							buildEnvironments.add(environment);
						}
					}
				}
			}
			// Temporarily reinject this environment by attaching an
			// action to the build. Be aware that the order these
			// variables are applied may differ from that of the
			// original build environment.
			EnvironmentContributingAction environmentAction = new EnvironmentContributingAction() {
				public void buildEnvVars(AbstractBuild<?,?> build, EnvVars env) {
					// Apply global and node properties
					for (Environment environment : buildEnvironments) {
						environment.buildEnvVars(env);
					}
				}
				public String getDisplayName() { return null; }
				public String getIconFileName() { return null; }
				public String getUrlName() { return null; }
			};

			try {
				build.getActions().add(environmentAction);
				Map<String, String> env = build.getEnvironment(listener);
				if (var.length() == 0) {
					return env.toString();
				} else {
					String value = env.get(var);
					if (value == null) {
						value = "";
					}
					return value;
				}
			} finally {
				build.getActions().remove(environmentAction);
			}
		} finally {
			for (Environment environment : buildEnvironments) {
				environment.tearDown(build, listener);
			}
		}
	}

	public boolean hasNestedContent() {
		return false;
	}
	
}
