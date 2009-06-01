package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SVNRevisionContent implements EmailContent {
	
	private static final String TOKEN = "SVN_REVISION";

	public String getToken() {
		return TOKEN;
	}
	
	public List<String> getArguments() {
		return Collections.emptyList();
	}
	
	public String getHelpText() {
		return "Displays the subversion revision number.";
	}
	
	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
			EmailType emailType, Map<String, ?> args) {

                Map<String, String> env = build.getEnvVars();
	        String value = env.get("SVN_REVISION");
                if (value == null) {
                   value = "400";
                }
                return value;
	}

	public boolean hasNestedContent() {
		return false;
	}

}
