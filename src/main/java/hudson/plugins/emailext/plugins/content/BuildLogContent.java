package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.tasks.Mailer;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An EmailContent for build log. Shows last 250 lines of the build log file.
 * 
 * @author dvrzalik
 */
public class BuildLogContent implements EmailContent {
	
	private static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

	private static final String TOKEN = "BUILD_LOG";
	
	private static final String MAX_LINES_ARG_NAME = "maxLines";
	private static final int MAX_LINES_DEFAULT_VALUE = 250;
	
	public String getToken() {
		return TOKEN;
	}

	public List<String> getArguments() {
		return Collections.singletonList(MAX_LINES_ARG_NAME);
	}
	
	public String getHelpText() {
		return "Displays the end of the build log.\n" +
		"<ul>\n" +
		
		"<li><i>" + MAX_LINES_ARG_NAME + "</i> - display at most this many " +
		"lines of the log.<br>\n" +
		"Defaults to " + MAX_LINES_DEFAULT_VALUE + ".\n" +
		
		"</ul>\n";
	}

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, EmailType emailType,
			Map<String, ?> args) {
		
		StringBuffer buffer = new StringBuffer();
		// getLog() chokes and dies if called with a number <= 0.
		int maxLines = Math.max(Args.get(args, MAX_LINES_ARG_NAME, MAX_LINES_DEFAULT_VALUE), 1);
		try {
			List<String> lines = build.getLog(maxLines);
			for(String line: lines) {
				//TODO: show file links the same way as MailSender
				buffer.append(line);
				buffer.append('\n');
			}
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		
		return buffer.toString();
	}

	public boolean hasNestedContent() {
		return false;
	}
	
}
