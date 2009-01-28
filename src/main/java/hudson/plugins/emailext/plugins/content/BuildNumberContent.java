package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BuildNumberContent implements EmailContent {
	
	private static final String TOKEN = "BUILD_NUMBER";

	public String getToken() {
		return TOKEN;
	}

	public List<String> getArguments() {
		return Collections.emptyList();
	}
	
	public String getHelpText() {
		return "Displays the number of the current build.";
	}
	
	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, EmailType emailType,
			Map<String, ?> args) {
		return String.valueOf(build.getNumber());
	}

	public boolean hasNestedContent() {
		return false;
	}

}
