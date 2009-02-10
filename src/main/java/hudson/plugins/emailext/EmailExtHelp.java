package hudson.plugins.emailext;

import hudson.plugins.emailext.plugins.EmailContent;

/**
 * Produces all the dynamically generated help text.
 */
public class EmailExtHelp {

	/**
	 * Generates the help text for the content tokens available
	 * while configuring a project.
	 * 
	 * @return the help text
	 */
	public static String getContentTokenHelpText() {
		return getTokenHelpText(true);
	}
	
	/**
	 * Generates the help text for the content tokens available
	 * while doing global configuration.
	 * 
	 * @return the help text
	 */
	public static String getGlobalContentTokenHelpText() {
		return getTokenHelpText(false);
	}
	
	private static String getTokenHelpText(boolean displayDefaultTokens) {
		StringBuffer sb = new StringBuffer();
		
		// This is the help for the content tokens
		sb.append("\n" +
				"<p>Arguments may be given for each token in the form name=<i>value</i>, " +
				"and all arguments are optional.  The {'s and }'s may be omitted if there are " +
				"no arguments.  Examples: $TOKEN, ${TOKEN}, ${TOKEN, count=100}.</p>\n" +
				"<b>Available Tokens</b>\n" +
				"<ul>\n");
		
		if (displayDefaultTokens) {
			sb.append(
				"<li><b>${DEFAULT_SUBJECT} - </b> This is the default email subject that is " +
				"configured in Hudson's system configuration page. </li>\n" +
				"<li><b>${DEFAULT_CONTENT} - </b> This is the default email content that is " +
				"configured in Hudson's system configuration page. </li>\n" +
				"<li><b>${PROJECT_DEFAULT_SUBJECT} - </b> This is the default email subject for " +
				"this project.  The result of using this token in the advanced configuration is " +
				"what is in the Default Subject field above. WARNING: Do not use this token in the " +
				"Default Subject or Content fields.  Doing this has an undefined result. </li>\n" +
				"<li><b>${PROJECT_DEFAULT_CONTENT} - </b> This is the default email content for " +
				"this project.  The result of using this token in the advanced configuration is " +
				"what is in the Default Content field above. WARNING: Do not use this token in the " +
				"Default Subject or Content fields.  Doing this has an undefined result. </li>\n");
		}
		
		for (EmailContent content : ExtendedEmailPublisher.getEmailContentTypes()) {
			sb.append("<li><b>${");
			sb.append(content.getToken());
			for (String arg : content.getArguments()) {
				sb.append(", <i>");
				sb.append(arg);
				sb.append("</i>");
			}
			sb.append("} - </b>");
			sb.append(content.getHelpText());
			sb.append("</li>\n");
		}
		sb.append("</ul>\n");
		
		return sb.toString();
	}
	
}
