package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Project;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.scm.ChangeLogSet;

public class ChangesSinceLastBuildContent implements EmailContent {
	
	private static final String TOKEN = "CHANGES";

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(
			AbstractBuild<P, B> build,
			EmailType emailType) {
		
		StringBuffer buf= new StringBuffer();
        for (ChangeLogSet.Entry entry : build.getChangeSet()) {
            buf.append('[');
            buf.append(entry.getAuthor().getFullName());
            buf.append("] ");
            String m = entry.getMsg();
            buf.append(m);
            if (!m.endsWith("\n")) {
                buf.append('\n');
            }
            buf.append('\n');
        }
        
        return buf.toString();
	}

	public String getToken() {
		return TOKEN;
	}

	public boolean hasNestedContent() {
		return false;
	}

	public String getHelpText() {
		return "Displays the changes since the last build. (Not implemented yet)";
	}

}
