package hudson.plugins.emailext.plugins.content;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HudsonURLContent implements EmailContent {
	
	private static final String TOKEN = "HUDSON_URL";

	public String getToken() {
		return TOKEN;
	}
	
	public List<String> getArguments() {
		return Collections.emptyList();
	}
	
	public String getHelpText() {
		return "Displays the URL to the Hudson server. (You can change this on the system configuration page.)";
	}
	
	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
			EmailType emailType, Map<String, ?> args) {
		String hudsonUrl = ExtendedEmailPublisher.DESCRIPTOR.getHudsonUrl();
		if(hudsonUrl==null) return "";
		if(!hudsonUrl.endsWith("/"))
			hudsonUrl += "/";
		
		return Util.encode(hudsonUrl);
	}

	public boolean hasNestedContent() {
		return false;
	}
}
