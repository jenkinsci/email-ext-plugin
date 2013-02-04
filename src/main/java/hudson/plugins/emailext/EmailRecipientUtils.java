package hudson.plugins.emailext;

import hudson.EnvVars;
import hudson.model.User;
import hudson.tasks.Mailer;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.Set;

public class EmailRecipientUtils {

    public static final String COMMA_SEPARATED_SPLIT_REGEXP = "[,\\s]+";

    public static final int TO = 0;
    public static final int CC = 1;
    
	
    /**
    * format Name address  
    * @param name 
    * @param email Email address
    * @return the formatted address
    */
    public static String formatAddress(String name, String email) {
	    if (name.equalsIgnoreCase("")) {
		    return email;
	    }
	    try {
		    return String.format("%1$s <%2$s>", MimeUtility.encodeText(name, "UTF-8", "B"), email);
		} catch (UnsupportedEncodingException e) {
		    e.printStackTrace();
	    }
	    return email;
    }
	
    public Set<InternetAddress> convertRecipientString(String recipientList, EnvVars envVars)
            throws AddressException {
        return convertRecipientString(recipientList, envVars, TO);
    }
    
    public Set<InternetAddress> convertRecipientString(String recipientList, EnvVars envVars, int type)
        throws AddressException{
        final Set<InternetAddress> internetAddresses = new LinkedHashSet<InternetAddress>();
        if (!StringUtils.isBlank(recipientList)) {
            final String expandedRecipientList = envVars.expand(recipientList);
            final String[] addresses = StringUtils.trim(expandedRecipientList).split(COMMA_SEPARATED_SPLIT_REGEXP);
            final String defaultSuffix = Mailer.descriptor().getDefaultSuffix();
            for (String address : addresses) {
                if(!StringUtils.isBlank(address)) {
                    boolean isCc = false;
                    address = address.trim();
    
                    isCc = address.startsWith("cc:");
                    // if not a valid address, check if there is a default suffix (@something.com) provided
                    if (!address.contains("@")){
                        User u = User.get(address, false);
                        String userEmail = null;
                        if(u != null) {
                            userEmail = GetUserConfiguredEmail(u);
                            if(userEmail != null){
                                //if configured user email does not have @domain prefix, then default prefix will be added on next step
                                address = userEmail;
                            }
                        }
                    }
                    if (!address.contains("@") && defaultSuffix != null && defaultSuffix.contains("@")) {
                        address += defaultSuffix;
                    }

					
                    String[] arr = address.split("<");
                    if(address.endsWith(">")&&(arr.length>1)){
	
	                   String name = arr[0];
	                   String mail = arr[1].substring(0, arr[1].length()-1);
	                   address = formatAddress(name,mail);
	
                    }
					
                    if(isCc) {
                        address = address.substring(3);
                    }
    
                    if((type == TO && !isCc) || (type == CC && isCc)) {
                        internetAddresses.add(new InternetAddress(address));
                    }
                }
            }
        }
        return internetAddresses;
    }

    public static String GetUserConfiguredEmail(User user) {
        String addr = null;
        if(user != null) {
            Mailer.UserProperty mailProperty = user.getProperty(Mailer.UserProperty.class);
            if (mailProperty != null) {
                addr = mailProperty.getAddress();
            }
        }
        return addr;
    }

    public FormValidation validateFormRecipientList(String recipientList) {
        // Try and convert the recipient string to a list of InternetAddress. If this fails then the validation fails.
        try {
            convertRecipientString(recipientList, new EnvVars(), TO);
            convertRecipientString(recipientList, new EnvVars(), CC);
            return FormValidation.ok();
        } catch (AddressException e) {
            return FormValidation.error(e.getMessage() + ": \"" + e.getRef() + "\"");
        }
    }
}
