package hudson.plugins.emailext.plugins.content;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Project;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;

public class HudsonURLContent implements EmailContent {
	
	private static final String TOKEN = "HUDSON_URL";

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(
			AbstractBuild<P, B> build,
			EmailType emailType) {
		String hudsonUrl = ExtendedEmailPublisher.DESCRIPTOR.getHudsonUrl();
		if(!hudsonUrl.endsWith("/"))
			hudsonUrl += "/";
		
		return Util.encode(hudsonUrl);
	}

	public String getToken() {
		return TOKEN;
	}

	public boolean hasNestedContent() {
		return false;
	}

	public String getHelpText() {
		return "Displays the URL to the Hudson server. (You can change this on the system configuration page.)";
	}

}
