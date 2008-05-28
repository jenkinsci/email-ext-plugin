package hudson.plugins.emailext.plugins.content;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Project;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;

public class BuildURLContent implements EmailContent {
	
	private static final String TOKEN = "BUILD_URL";

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(
			AbstractBuild<P, B> build,
			EmailType emailType) {
		return "$HUDSON_URL/" + Util.encode(build.getUrl());
	}

	public String getToken() {
		return TOKEN;
	}

	public boolean hasNestedContent() {
		return true;
	}

	public String getHelpText() {
		return "Displays the URL to the current build.";
	}

}
