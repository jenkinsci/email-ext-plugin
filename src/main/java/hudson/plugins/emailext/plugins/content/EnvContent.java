package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;

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
			EmailType emailType, Map<String, ?> args) {
		String var = Args.get(args, VAR_ARG_NAME, VAR_DEFAULT_VALUE);

		Map<String, String> env = build.getEnvVars();
		if (var.length() == 0) {
			return env.toString();
		} else {
			String value = env.get(var);
			if (value == null) {
				value = "";
			}
			return value;
		}
	}

	public boolean hasNestedContent() {
		return false;
	}
	
}
