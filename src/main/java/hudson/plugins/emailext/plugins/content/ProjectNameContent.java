package hudson.plugins.emailext.plugins.content;

import hudson.model.Build;
import hudson.model.Project;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;

public class ProjectNameContent implements EmailContent {
	
	private static final String TOKEN = "PROJECT_NAME";

	public <P extends Project<P, B>, B extends Build<P, B>> String getContent(
			Build<P, B> build,
			EmailType emailType) {
		return build.getProject().getName();
	}

	public String getToken() {
		return TOKEN;
	}

	public boolean hasNestedContent() {
		return false;
	}

	public String getHelpText() {
		return "Displays the project's name.";
	}

}
