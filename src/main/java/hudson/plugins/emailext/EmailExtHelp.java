package hudson.plugins.emailext;

import hudson.plugins.emailext.plugins.ContentBuilder;
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
        StringBuilder sb = new StringBuilder();

        // This is the help for the content tokens
        sb.append("\n"
                + "<p>All arguments are optional. Arguments may be given for each token in the "
                + "form <i>name=\"value\"</i> for strings and in the form <i>name=value</i> for booleans and numbers.  "
                + "In string arguments, escape '\"', '\\', and line terminators ('\n' or '\r\n') with a '\\', "
                + "e.g. <i>arg1=\"\\\"quoted\\\"\"</i>; <i>arg2=\"c:\\\\path\"</i>; and <i>arg3=\"one\\<br/>two\"</i>.  "
                + "The {'s and }'s may be omitted if there are no arguments.</p>"
                + "<p>Examples: $TOKEN, ${TOKEN}, ${TOKEN, count=100}, ${ENV, var=\"PATH\"}</p>\n"
                + "<b>Available email-ext Tokens</b>\n"
                + "<dl>\n");

        if (displayDefaultTokens) {
            sb.append(
                    "<dt>${DEFAULT_SUBJECT}</dt><dd>This is the default email subject that is "
                    + "configured in Jenkins's system configuration page. </dd>\n"
                    + "<dt>${DEFAULT_CONTENT}</dt><dd>This is the default email content that is "
                    + "configured in Jenkins's system configuration page. </dd>\n"
                    + "<dt>${PROJECT_DEFAULT_SUBJECT}</dt><dd>This is the default email subject for "
                    + "this project.  The result of using this token in the advanced configuration is "
                    + "what is in the Default Subject field above. WARNING: Do not use this token in the "
                    + "Default Subject or Content fields.  Doing this has an undefined result. </dd>\n"
                    + "<dt>${PROJECT_DEFAULT_CONTENT}</dt><dd>This is the default email content for "
                    + "this project.  The result of using this token in the advanced configuration is "
                    + "what is in the Default Content field above. WARNING: Do not use this token in the "
                    + "Default Subject or Content fields.  Doing this has an undefined result. </dd>\n");
        }

        for (EmailContent content : ContentBuilder.getEmailContentTypes()) {
            sb.append("<dt>${");
            sb.append(content.getToken());
            for (String arg : content.getArguments()) {
                sb.append(", <i>");
                sb.append(arg);
                sb.append("</i>");
            }
            sb.append("}</dt><dd>");
            sb.append(content.getHelpText());
            sb.append("</dd>\n");
        }
        sb.append("</dl>\n");

        return sb.toString();
    }
}
