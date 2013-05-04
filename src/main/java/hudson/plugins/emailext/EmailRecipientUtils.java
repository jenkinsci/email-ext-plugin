package hudson.plugins.emailext;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

import hudson.EnvVars;
import hudson.model.User;
import hudson.tasks.Mailer;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;

public class EmailRecipientUtils {

    private static final Logger LOGGER = Logger.getLogger(EmailRecipientUtils.class.getName());

    public static final String COMMA_SEPARATED_SPLIT_REGEXP = "[,\\s]+";

    public static final int TO = 0;
    public static final int CC = 1;
    
    public Set<InternetAddress> convertRecipientString(String recipientList, EnvVars envVars)
            throws AddressException, UnsupportedEncodingException {
        return convertRecipientString(recipientList, envVars, TO);
    }
    
    public Set<InternetAddress> convertRecipientString(String recipientList, EnvVars envVars, int type)
        throws AddressException, UnsupportedEncodingException {
        final Set<InternetAddress> internetAddresses = new LinkedHashSet<InternetAddress>();
        if (!StringUtils.isBlank(recipientList)) {
            final String expandedRecipientList = fixupSpaces(envVars.expand(recipientList));
            InternetAddress[] all = InternetAddress.parse(expandedRecipientList.replace("cc:", ""));
            final Set<InternetAddress> to = new LinkedHashSet<InternetAddress>();
            final Set<InternetAddress> cc = new LinkedHashSet<InternetAddress>();
            final String defaultSuffix = Mailer.descriptor().getDefaultSuffix();

            for(InternetAddress address : all) {
                if(address.getPersonal() != null) {
                    if(expandedRecipientList.contains("cc:" + address.getPersonal()) || expandedRecipientList.contains("cc:\"" + address.toString() + "\"")) {
                        cc.add(address);
                    } else {
                        to.add(address);
                    }
                } else {
                    if(expandedRecipientList.contains("cc:" + address.toString())) {
                        cc.add(address);
                    } else {
                        to.add(address);
                    }
                }
            }

            if(type == CC) {
                internetAddresses.addAll(cc);
            } else {
                internetAddresses.addAll(to);
            }

            for(InternetAddress address : internetAddresses) {
                if(!address.getAddress().contains("@")) {
                    User u = User.get(address.getAddress(), false);
                    String userEmail = null;
                    if(u != null) {
                        userEmail = getUserConfiguredEmail(u);
                        if(userEmail != null){
                            //if configured user email does not have @domain prefix, then default prefix will be added on next step
                            address.setAddress(userEmail);
                        }
                    }
                }

                if(!address.getAddress().contains("@") && defaultSuffix != null && defaultSuffix.contains("@")) {
                    address.setAddress(address.getAddress() + defaultSuffix);
                }

                if(address.getPersonal() != null) {
                    address.setPersonal(MimeUtility.encodeWord(address.getPersonal(), "UTF-8", "B"));
                }
            }
        }
        return internetAddresses;
    }

    public static String getUserConfiguredEmail(User user) {
        String addr = null;
        if(user != null) {
            Mailer.UserProperty mailProperty = user.getProperty(Mailer.UserProperty.class);
            if (mailProperty != null) {
                addr = mailProperty.getAddress();
                String message = String.format("Resolved %s to %s", user.getId(), addr);
                LOGGER.fine(message);
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
        } catch(UnsupportedEncodingException e) {
            return FormValidation.error(e.getMessage());
        }
    }

    private String fixupSpaces(String input) {
        input = input.replaceAll("\\s+", " ");
        if(input.contains(" ") && !input.contains(",")) {
            input = input.replace(" ", ",");
        }
        return input;
    }
}
