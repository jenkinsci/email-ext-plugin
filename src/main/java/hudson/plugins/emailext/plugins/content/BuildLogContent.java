package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.tasks.Mailer;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.util.Arrays;
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

	public static final String TOKEN = "BUILD_LOG";
	
    public static final String MAX_LINES_ARG_NAME = "maxLines";

    public static final String ESCAPE_HTML_ARG_NAME = "escapeHtml";

    public static final int MAX_LINES_DEFAULT_VALUE = 250;

    public static final boolean ESCAPE_HTML_DEFAULT_VALUE = false;
	
	public String getToken() {
		return TOKEN;
	}

	public List<String> getArguments() {
        return Arrays.asList(MAX_LINES_ARG_NAME, ESCAPE_HTML_ARG_NAME);
	}
	
	public String getHelpText() {
		return "Displays the end of the build log.\n" +
		"<ul>\n" +
		
		"<li><i>" + MAX_LINES_ARG_NAME + "</i> - display at most this many " +
		"lines of the log.<br>\n" +
		"Defaults to " + MAX_LINES_DEFAULT_VALUE + ".\n" +

        "<li><i>" + ESCAPE_HTML_ARG_NAME + "</i> - If true, HTML is escaped.<br>\n" +
        "Defaults to " + ESCAPE_HTML_DEFAULT_VALUE + ".\n" +
		
		"</ul>\n";
	}

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
			EmailType emailType, Map<String, ?> args) {
		
		// getLog() chokes and dies if called with a number <= 0.
		final int maxLines = Math.max(Args.get(args, MAX_LINES_ARG_NAME, MAX_LINES_DEFAULT_VALUE), 1);
        final boolean escapeHtml = Args.get( args, ESCAPE_HTML_ARG_NAME, ESCAPE_HTML_DEFAULT_VALUE );

        StringBuffer buffer = new StringBuffer();
		try {
			List<String> lines = build.getLog(maxLines);
			for(String line: lines) {
                if (escapeHtml) {
                    line = StringEscapeUtils.escapeHtml( line );
                }
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
