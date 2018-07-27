package hudson.plugins.emailext;

import javax.mail.internet.InternetAddress;

public class RecipientListStringAnalyser {

    private final String recipients;

    public RecipientListStringAnalyser(String recipientsListString) {
        this.recipients = recipientsListString;
    }

    public int getType(InternetAddress address) {
        int type = EmailRecipientUtils.TO;
        if(address.getPersonal() != null) {
            if(recipients.contains("bcc:" + address.getPersonal()) || recipients.contains("bcc:\"" + address.toString() + "\"")) {
                type = EmailRecipientUtils.BCC;
            } else if(recipients.contains("cc:" + address.getPersonal()) || recipients.contains("cc:\"" + address.toString() + "\"")) {
                type = EmailRecipientUtils.CC;
            } else {
                type = EmailRecipientUtils.TO;
            }
        } else {
            if(recipients.contains("bcc:" + address.toString())) {
                type = EmailRecipientUtils.BCC;
            } else if(recipients.contains("cc:" + address.toString())) {
                type = EmailRecipientUtils.CC;
            } else {
                type = EmailRecipientUtils.TO;
            }
        }
        return type;
    }
}
