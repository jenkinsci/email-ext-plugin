package hudson.plugins.emailext.plugins.content;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;

public class BuildURLContent implements EmailContent {
	
	private static final String TOKEN = "BUILD_URL";

	public String getToken() {
		return TOKEN;
	}

	public List<String> getArguments() {
		return Collections.emptyList();
	}
	
	public String getHelpText() {
		return "Displays the URL to the current build.";
	}

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, EmailType emailType,
			Map<String, ?> args) {
		return "${HUDSON_URL}" + Util.encode(build.getUrl());
	}
	
	public boolean hasNestedContent() {
		return true;
	}

}
