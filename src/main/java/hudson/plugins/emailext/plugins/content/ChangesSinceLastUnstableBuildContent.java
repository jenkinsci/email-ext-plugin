package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.Util;
import hudson.plugins.emailext.Util.PrintfSpec;
import hudson.plugins.emailext.plugins.EmailContent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangesSinceLastUnstableBuildContent implements EmailContent {
	
	private static final String TOKEN = "CHANGES_SINCE_LAST_UNSTABLE";
	
	private static final String REVERSE_ARG_NAME = "reverse";
	private static final boolean REVERSE_DEFAULT_VALUE = false;
	
	private static final String FORMAT_ARG_NAME = "format";
	private static final String FORMAT_DEFAULT_VALUE = "Changes for Build #%n\\n%c\\n";
	
	private static final String SHOW_PATHS_ARG_NAME = "showPaths";
	private static final String CHANGES_FORMAT_ARG_NAME = "changesFormat";
	private static final String PATH_FORMAT_ARG_NAME = "pathFormat";

	public String getToken() {
		return TOKEN;
	}

	public List<String> getArguments() {
		return Arrays.asList(REVERSE_ARG_NAME, FORMAT_ARG_NAME, SHOW_PATHS_ARG_NAME, CHANGES_FORMAT_ARG_NAME, PATH_FORMAT_ARG_NAME);
	}
	
	public String getHelpText() {
		return "Displays the changes since the last unstable or successful build.\n" +
			"<ul>\n" +
			
			"<li><i>" + REVERSE_ARG_NAME + "</i> - indicates that " +
			"most recent builds should be at the top.<br>\n" +
			"Defaults to " + REVERSE_DEFAULT_VALUE + ".\n" +
			
			"<li><i>" + FORMAT_ARG_NAME + "</i> - for each build listed, " +
			"a string containing %X, where %X is one of %c for changes, " +
			"or %n for build number.<br>\n" +
			"Defaults to \"" + FORMAT_DEFAULT_VALUE + "\".\n" +
			
			"<li><i>" + SHOW_PATHS_ARG_NAME + "</i>, <i>" + CHANGES_FORMAT_ARG_NAME + "</i>, <i>" + PATH_FORMAT_ARG_NAME + "</i> - " +
			"defined as <i>showPaths</i>, <i>format</i>, and <i>pathFormat</i> " +
			"from ${CHANGES}, repectively.\n" +
			
			"</ul>\n";
	}

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
			EmailType emailType, Map<String, ?> args) {
		if (build.getPreviousBuild() == null) {
			return "";
		}

		AbstractBuild<P,B> firstIncludedBuild = build;
		{
			B prev = firstIncludedBuild.getPreviousBuild();
			while (prev != null && prev.getResult().isWorseThan(Result.UNSTABLE)) {
				firstIncludedBuild = prev;
				prev = firstIncludedBuild.getPreviousBuild();
			}
		}
		
		String formatString = Args.get(args, FORMAT_ARG_NAME, FORMAT_DEFAULT_VALUE);
		boolean reverseOrder = Args.get(args, REVERSE_ARG_NAME, REVERSE_DEFAULT_VALUE);
		
		Map<String, Object> childArgs = new HashMap<String, Object>();
		childArgs.put(FORMAT_ARG_NAME, args.get(CHANGES_FORMAT_ARG_NAME));
		childArgs.put(PATH_FORMAT_ARG_NAME, args.get(PATH_FORMAT_ARG_NAME));

		StringBuffer sb = new StringBuffer();
		final AbstractBuild<P, B> startBuild;
		final AbstractBuild<P, B> endBuild;
		if (reverseOrder) {
			startBuild = build;
			endBuild = firstIncludedBuild;
		} else {
			startBuild = firstIncludedBuild;
			endBuild = build;
		}
		AbstractBuild<P,B> currentBuild = null;
		while(currentBuild != endBuild) {
			if (currentBuild == null) {
				currentBuild = startBuild;
			} else {
				if (reverseOrder) {
					currentBuild = currentBuild.getPreviousBuild();
				} else {
					currentBuild = currentBuild.getNextBuild();
				}
			}
			appendBuild(sb, formatString, publisher, emailType, currentBuild, childArgs);
		}
		
		return sb.toString();
	}

	private <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	void appendBuild(StringBuffer buf, String formatString, final ExtendedEmailPublisher publisher, final EmailType emailType,
			final AbstractBuild<P, B> currentBuild, final Map<String, Object> childArgs) {
		// Use this object since it already formats the changes per build
		final ChangesSinceLastBuildContent changes = new ChangesSinceLastBuildContent();
		
		Util.printf(buf, formatString, new PrintfSpec() {

			public boolean printSpec(StringBuffer buf, char formatChar) {
				switch (formatChar) {
				case 'c':
					buf.append(changes.getContent(currentBuild, publisher, emailType, childArgs));
					return true;
				case 'n':
					buf.append(currentBuild.getNumber());
					return true;
				default:
					return false;
				}
			}
			
		});
	}

	public boolean hasNestedContent() {
		return false;
	}

}
