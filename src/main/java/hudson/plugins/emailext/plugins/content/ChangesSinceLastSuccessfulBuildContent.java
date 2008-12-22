package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Project;
import hudson.model.Result;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;

public class ChangesSinceLastSuccessfulBuildContent implements EmailContent {
	
	private static final String TOKEN = "CHANGES_SINCE_LAST_SUCCESS";

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(
			AbstractBuild<P, B> build,
			EmailType emailType) {
		if (build.getPreviousBuild() == null) {
			return "";
		}
		
		//Use this object since it already formats the changes per build
		ChangesSinceLastBuildContent changes = new ChangesSinceLastBuildContent();
		
		AbstractBuild<P,B> firstIncludedBuild = build;
		{
			B prev = firstIncludedBuild.getPreviousBuild();
			while (prev != null && prev.getResult() == Result.FAILURE) {
				firstIncludedBuild = prev;
				prev = firstIncludedBuild.getPreviousBuild();
			}
		}
		
		StringBuffer sb = new StringBuffer();
		
		AbstractBuild<P,B> currentIncludedBuild = null;
		while(currentIncludedBuild != build) {
			if (currentIncludedBuild == null) {
				currentIncludedBuild = firstIncludedBuild;
			} else {
				currentIncludedBuild = currentIncludedBuild.getNextBuild();
			}
			sb.append("Changes for Build #");
			sb.append(currentIncludedBuild.getNumber());
			sb.append("\n");
			sb.append(changes.getContent(currentIncludedBuild, emailType));
			sb.append("\n");
		}
		
        return sb.toString();
	}

	public String getToken() {
		return TOKEN;
	}

	public boolean hasNestedContent() {
		return false;
	}

	public String getHelpText() {
		return "Displays the changes since the last successful build.";
	}

}
