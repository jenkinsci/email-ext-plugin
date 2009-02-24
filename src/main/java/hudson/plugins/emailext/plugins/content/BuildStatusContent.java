package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BuildStatusContent implements EmailContent {
	
	private static final String TOKEN = "BUILD_STATUS";

	public String getToken() {
		return TOKEN;
	}
	
	public List<String> getArguments() {
		return Collections.emptyList();
	}
	
	public String getHelpText() {
		return "Displays the status of the current build. (failing, success, etc...)";
	}
	
	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
			EmailType emailType, Map<String, ?> args) {
		Result buildResult = build.getResult();

		if (buildResult == Result.FAILURE) {
			B prevBuild = build.getPreviousBuild();
			if (prevBuild != null && (prevBuild.getResult() == Result.FAILURE)) {
				return "Still Failing";
			} else {
				return "Failure";
			}
		} else if (buildResult == Result.UNSTABLE) {
			B prevBuild = build.getPreviousBuild();
			if (prevBuild != null && (prevBuild.getResult() == Result.UNSTABLE)) {
				return "Still Unstable";
			} else {
				return "Unstable";
			}
		} else if (buildResult == Result.SUCCESS) {
			B prevBuild = build.getPreviousBuild();
			if (prevBuild != null && (prevBuild.getResult() == Result.UNSTABLE || prevBuild.getResult() == Result.FAILURE)) {
				return "Fixed";
			} else {
				return "Successful";
			}
		}
		
		return "Unknown";
	}

	public boolean hasNestedContent() {
		return false;
	}

}
