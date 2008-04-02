package hudson.plugins.emailext.plugins.content;

import hudson.model.Build;
import hudson.model.Project;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;

public class BuildNumberContent implements EmailContent {
	
	private static final String TOKEN = "BUILD_NUMBER";

	public <P extends Project<P, B>, B extends Build<P, B>> String getContent(
			Build<P, B> build,
			EmailType emailType) {
		return String.valueOf(build.getNumber());
	}

	public String getToken() {
		return TOKEN;
	}

	public boolean hasNestedContent() {
		return false;
	}

	public String getHelpText() {
		return "Displays the number of the current build.";
	}

}
