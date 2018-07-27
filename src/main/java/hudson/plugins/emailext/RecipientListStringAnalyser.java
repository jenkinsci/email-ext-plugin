package hudson.plugins.emailext;

import javax.mail.internet.InternetAddress;

public class RecipientListStringAnalyser {

    static final int NOT_FOUND = -1;

    private final String recipients;

    private int idx = 0;

    public RecipientListStringAnalyser(String recipientsListString) {
        this.recipients = recipientsListString;
    }

    public int getType(InternetAddress address) {
        int type = NOT_FOUND;
        if (address.getPersonal() != null) {
            if (recipients.contains("bcc:" + address.getPersonal()) || recipients.contains("bcc:\"" + address.toString() + "\"")) {
                type = EmailRecipientUtils.BCC;
            } else if (recipients.contains("cc:" + address.getPersonal()) || recipients.contains("cc:\"" + address.toString() + "\"")) {
                type = EmailRecipientUtils.CC;
            } else if (recipients.contains(address.getPersonal()) || recipients.contains("\"" + address.toString() + "\"")) {
                type = EmailRecipientUtils.TO;
            }
        } else {
            if(recipients.contains("bcc:" + address.toString())) {
                type = EmailRecipientUtils.BCC;
            } else if(recipients.contains("cc:" + address.toString())) {
                type = EmailRecipientUtils.CC;
            } else if(recipients.contains(address.toString())) {
                type = EmailRecipientUtils.TO;
            }
        }
        return type;
    }
}
