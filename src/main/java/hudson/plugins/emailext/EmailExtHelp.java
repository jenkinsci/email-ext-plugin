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
        sb.append("\n");
        sb.append("<p>All arguments are optional. Arguments may be given for each token in the ");
        sb.append("form <i>name=\"value\"</i> for strings and in the form <i>name=value</i> for booleans and numbers. ");
        sb.append("The {'s and }'s may be omitted if there are no arguments.</p>");
        sb.append("<p>Examples: $TOKEN, ${TOKEN}, ${TOKEN, count=100}, ${ENV, var=\"PATH\"}</p>\n");
        sb.append("<b>Available Tokens</b>\n");
        sb.append("<ul>\n");

        if (displayDefaultTokens) {
            sb.append("<li><b>${DEFAULT_SUBJECT} - </b> This is the default email subject that is ");
            sb.append("configured in Jenkins's system configuration page. </li>\n");
            sb.append("<li><b>${DEFAULT_CONTENT} - </b> This is the default email content that is ");
            sb.append("configured in Jenkins's system configuration page. </li>\n");
            sb.append("<li><b>${PROJECT_DEFAULT_SUBJECT} - </b> This is the default email subject for ");
            sb.append("this project. The result of using this token in the advanced configuration is ");
            sb.append("what is in the Default Subject field above. WARNING: Do not use this token in the ");
            sb.append("Default Subject or Content fields. Doing this has an undefined result. </li>\n");
            sb.append("<li><b>${PROJECT_DEFAULT_CONTENT} - </b> This is the default email content for ");
            sb.append("this project. The result of using this token in the advanced configuration is ");
            sb.append("what is in the Default Content field above. WARNING: Do not use this token in the ");
            sb.append("Default Subject or Content fields. Doing this has an undefined result. </li>\n");
        }

        for (EmailContent content : ContentBuilder.getEmailContentTypes()) {
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
        sb.append("<li><b>TOKEN MACROS - </b> Tokens from plugins that produce token macros. See <a target='_blank' ");
        sb.append("href='https://wiki.jenkins-ci.org/display/JENKINS/Token+Macro+Plugin#TokenMacroPlugin-Pluginsthatproducetokens'>");
        sb.append("Plugins that produce tokens</a> for available tokens.</li>\n");

        sb.append("</ul>\n");

        return sb.toString();
    }
}
