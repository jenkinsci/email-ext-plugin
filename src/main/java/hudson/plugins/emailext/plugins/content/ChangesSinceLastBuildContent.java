package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.Util;
import hudson.plugins.emailext.Util.PrintfSpec;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.scm.ChangeLogSet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ChangesSinceLastBuildContent implements EmailContent {
	
	private static final String TOKEN = "CHANGES";
	
	private static final String SHOW_PATHS_ARG_NAME = "showPaths";
	private static final boolean SHOW_PATHS_DEFAULT_VALUE = false;
	
	private static final String FORMAT_ARG_NAME = "format";
	private static final String FORMAT_DEFAULT_VALUE = "[%a] %m\\n";
	private static final String FORMAT_DEFAULT_VALUE_WITH_PATHS = "[%a] %m%p\\n";
	
	private static final String PATH_FORMAT_ARG_NAME = "pathFormat";
	private static final String PATH_FORMAT_DEFAULT_VALUE = "\\t%p\\n";

	public String getToken() {
		return TOKEN;
	}
	
	public List<String> getArguments() {
		return Arrays.asList(SHOW_PATHS_ARG_NAME, FORMAT_ARG_NAME, PATH_FORMAT_ARG_NAME);
	}
	
	public String getHelpText() {
		return "Displays the changes since the last build.\n" +
		"<ul>\n" +
		
		"<li><i>" + SHOW_PATHS_ARG_NAME + "</i> - if true, the paths " +
		"modified by a commit are shown.<br>\n" +
		"Defaults to " + SHOW_PATHS_DEFAULT_VALUE + ".\n" +
		
		"<li><i>" + FORMAT_ARG_NAME + "</i> - for each commit listed, " +
		"a string containing %X, where %X is one of %a for author, " +
		"%d for date, %m for message, %p for paths, or %r for revision.  " +
		"Not all revision systems support %d and %r.  If specified, " +
		"<i>" + SHOW_PATHS_ARG_NAME + "</i> is ignored.<br>\n" +
		"Defaults to \"" + FORMAT_DEFAULT_VALUE + "\".\n" +
		
		"<li><i>" + PATH_FORMAT_ARG_NAME + "</i> - a string containing " +
		"%p to indicate how to print paths.<br>\n" +
		"Defaults to \"" + PATH_FORMAT_DEFAULT_VALUE + "\".\n" +
		
		"</ul>\n";
	}
	
	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, EmailType emailType,
			Map<String, ?> args) {
		boolean showPaths = Args.get(args, SHOW_PATHS_ARG_NAME, SHOW_PATHS_DEFAULT_VALUE);
		String formatStringDefault = showPaths ? FORMAT_DEFAULT_VALUE_WITH_PATHS : FORMAT_DEFAULT_VALUE;
		String formatString = Args.get(args, FORMAT_ARG_NAME, formatStringDefault);
		String pathFormatString = Args.get(args, PATH_FORMAT_ARG_NAME, PATH_FORMAT_DEFAULT_VALUE);
		
		StringBuffer buf = new StringBuffer();
		for (ChangeLogSet.Entry entry : build.getChangeSet()) {
			appendEntry(buf, entry, formatString, pathFormatString);
		}
		
		return buf.toString();
	}

	private void appendEntry(StringBuffer buf, final ChangeLogSet.Entry entry, final String formatString, final String pathFormatString) {
		Util.printf(buf, formatString, new PrintfSpec() {

			public boolean printSpec(StringBuffer buf, char formatChar) {
				switch (formatChar) {
				case 'a':
					buf.append(entry.getAuthor().getFullName());
					return true;
				case 'd': {
					try {
						Method getDateMethod = entry.getClass().getMethod("getDate");
						buf.append(getDateMethod.invoke(entry));
					} catch (NoSuchMethodException e) { // getMethod()
						// If it is not implemented, swallow the %d
					} catch (IllegalAccessException e) { // invoke()
						// Won't happen, getDate will be public
					} catch (InvocationTargetException e) { // invoke()
						// Won't happen, entry is the right type
					}
					return true;
				}
				case 'm': {
					String m = entry.getMsg();
					buf.append(m);
					if (!m.endsWith("\n")) {
						buf.append('\n');
					}
					return true;
				}
				case 'p': {
					Collection<String> affectedPaths = entry.getAffectedPaths();
					for (final String affectedPath : affectedPaths) {
						Util.printf(buf, pathFormatString, new PrintfSpec() {

							public boolean printSpec(StringBuffer buf, char formatChar) {
								if (formatChar == 'p') {
									buf.append(affectedPath);
									return true;
								} else {
									return false;
								}
							}
							
						});
					}
					return true;
				}
				case 'r': {
					try {
						Method getRevisionMethod = entry.getClass().getMethod("getRevision");
						buf.append(getRevisionMethod.invoke(entry));
					} catch (NoSuchMethodException e) { // getMethod()
						// If it is not implemented, swallow the %r
					} catch (IllegalAccessException e) { // invoke()
						// Won't happen, getDate will be public
					} catch (InvocationTargetException e) { // invoke()
						// Won't happen, entry is the right type
					}
					return true;
				}
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
