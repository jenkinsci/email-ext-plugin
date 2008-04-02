package hudson.plugins.emailext;

import hudson.plugins.emailext.plugins.EmailContent;

public class EmailExtHelp {
	
    //Renders the help text for the plugin.
    public static String getAdvancedEmailHelpText(){
    	StringBuffer sb = new StringBuffer();
    	sb.append("\n<table><tr>");
    	
    	//This is the help for the non-advanced settings
    	sb.append("<td><div><b>Help</b></div></td>" +
    			"<td>" +
    			"<p><b>Global Recipient List - </b>This is a comma or whitespace " +
    			"separated list of email addresses that should recieve emails for a" +
    			" trigger.</p>\n" +
    			
    			"<p><b>Default Subject - </b>This is the default email subject that " +
    			"will be used for each email that is sent. NOTE: this can be overridden " +
    			"in for each email trigger type in the Advanced section.</p>\n" +
    			
    			"<p><b>Default Content - </b>This is the default email content that " +
    			"will be used for each email that is sent. NOTE: this can be overridden " +
    			"in for each email trigger type in the Advanced section.</p>\n" +
    			"</td></tr>");
    	
    	//Put a space between help sections
    	sb.append("<tr><td colspan=\"2\"><br></td></tr>");
    	
    	//This is the help for the advanced settings
    	sb.append("<tr><td><div><b>Advanced Help</b></div></td>\n" +
    			"<td>" +
    			"<p><b>Trigger - </b>If configured, and trigger conditions are met, an" +
    			" email of this type will be sent upon completion of a build.  Click the" +
    			" help button next to the trigger name to find out more about what conditions" +
    			" will trigger an email of this type to be sent.</p>\n" +
    			
    			"<p><b>Send To Recipient List - </b>If this is checked, an email will" +
    			" be sent all email addresses in the global recipient list.</p>\n" +
    			
    			"<p><b>Send To Committers - </b>If this is checked, an email will" +
    			" be sent all users who are listed under \"changes\" for the build.  This" +
				" will use the default email suffix from Hudson's configuration page.</p>\n" +
				
    			"<p><b>More Configuration - </b>You can change more settings for each " +
    			"email trigger by clicking on the \"+(expand)\" link.</p>\n" +
    			
    			"<p><b>Remove - </b>You can remove an email trigger by clicking the " +
    			"\"Delete\" button on the row for the specified trigger. </p>\n" +
    			
    			"<p><b>Add a Trigger - </b>You can add an email trigger by selecting " +
    			"it from the dropdown menu.  This will add the trigger to the list of " +
    			"configurable triggers.</p>\n" +
    			"</td></tr>");

    	//Put a space between help sections
    	sb.append("<tr><td colspan=\"2\"><br></td></tr>");
    	
    	//This is the help for the per-email help configuration
    	sb.append("<tr>" +
    			"<td><div><b>More Configuration Help</b></div></td>" +
    			"<td>" +
    			"<p><b>Recipient List - </b>This is a comma or whitespace " +
    			"separated list of email addresses that should recieve emails for the" +
    			" selected trigger.</p>\n" +
    			
    			"<p><b>Subject - </b>This is email subject that " +
    			"will be used for the selected email trigger.</p>\n" +
    			
    			"<p><b>Content - </b>This is the default email content that " +
    			"will be used for the selected email trigger.</p>\n" +
    			"</td></tr>");
    	
    	//Put a space between help sections
    	sb.append("<tr><td colspan=\"2\"><br></td></tr>");
    	
    	sb.append("<tr><td><div><b>Content Tokens</b></div></td>\n" +
    			"<td><div>" +
    			"<p>You can specify the email subject line and body text by configuring " +
    			"the appropriate fields.  Furthermore, you can insert special text by placing" +
    			" tokens in these fields.  The list of available tokens, and a description of " +
    			"each can be found below.</p><br>\n" +
    			"<b>Available Tokens</b></div>\n" +
    			"<ul>\n" +
    			"<li><b>$DEFAULT_SUBJECT - </b> This is the default email subject that is " +
    			"configured in Hudson's system configuration page. </li>\n" +
    			"<li><b>$DEFAULT_CONTENT - </b> This is the default email content that is " +
				"configured in Hudson's system configuration page. </li>\n" +
				"<li><b>$PROJECT_DEFAULT_SUBJECT - </b> This is the default email subject for " +
				"this project.  The result of using this token in the advanced configuration is" +
				" what is in the Default Subject field below. WARNING: Do not use this token in the" +
				" Default Subject or Content fields.  Doing this has an undefined result. </li>\n" +
				"<li><b>$PROJECT_DEFAULT_CONTENT - </b> This is the default email content for " +
				"this project.  The result of using this token in the advanced configuration is" +
				" what is in the Default Content field below. WARNING: Do not use this token in the" +
				" Default Subject or Content fields.  Doing this has an undefined result. </li>\n");
    	for(EmailContent content : ExtendedEmailPublisher.getEmailContentTypes()){
    		sb.append("<li><b>$")
    			.append(content.getToken())
    			.append(" - </b>")
    			.append(content.getHelpText())
    			.append("</li>\n");
    	}
    	sb.append("</ul>\n");
    	sb.append("</td></tr>");
    	sb.append("</table>\n");
    	
    	return sb.toString();
    }

}
