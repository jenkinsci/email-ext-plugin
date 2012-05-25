package hudson.plugins.emailext;

import hudson.EnvVars;
import hudson.tasks.Mailer;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.LinkedHashSet;
import java.util.Set;

public class EmailRecepientUtils {

    public static final String COMMA_SEPARATED_SPLIT_REGEXP = "[,\\s]+";

    public Set<InternetAddress> convertRecipientString(String recipientList, EnvVars envVars)
            throws AddressException {
        final Set<InternetAddress> internetAddresses = new LinkedHashSet<InternetAddress>();
        if (StringUtils.isBlank(recipientList)) {
            return internetAddresses;
        }

        final String expandedRecipientList = envVars.expand(recipientList);
        final String[] addresses = StringUtils.trim(expandedRecipientList).split(COMMA_SEPARATED_SPLIT_REGEXP);
        final String defaultSuffix = Mailer.descriptor().getDefaultSuffix();
        for (String address : addresses) {
            if(!StringUtils.isBlank(address)) {
                // if not a valid address, check if there is a default suffix (@something.com) provided
                if (!address.contains("@") && defaultSuffix != null && defaultSuffix.contains("@")) {
                    address += defaultSuffix;
                }                
                internetAddresses.add(new InternetAddress(address));
            }
        }

        return internetAddresses;
    }

    public FormValidation validateFormRecipientList(String recipientList) {
        // Try and convert the recipient string to a list of InternetAddress. If this fails then the validation fails.
        try {
            convertRecipientString(recipientList, new EnvVars());
            return FormValidation.ok();
        } catch (AddressException e) {
            return FormValidation.error(e.getMessage() + ": \"" + e.getRef() + "\"");
        }
    }
}
