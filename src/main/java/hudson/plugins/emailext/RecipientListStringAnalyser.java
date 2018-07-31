package hudson.plugins.emailext;

import javax.mail.internet.InternetAddress;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

class RecipientListStringAnalyser {

    private static final Logger LOG = Logger.getLogger(RecipientListStringAnalyser.class.getName());

    static final int NOT_FOUND = -1;

    private final String recipients;

    private int idx = 0;

    RecipientListStringAnalyser(String recipientsListString) {
        this.recipients = recipientsListString;
        LOG.log(Level.FINE, MessageFormat.format("Analyzing: {0} ", recipients));
    }

    /**
     * Mind that this method must be called in order of the found InternetAddresses (from parsing the recipients list
     * string)!
     */
    int getType(InternetAddress address) {
        int type = NOT_FOUND;
        LOG.log(Level.FINE, MessageFormat.format("Looking for: {0}", address));
        LOG.log(Level.FINE, MessageFormat.format("...starting at: {0}", idx));
        int firstFoundIdx = findFirst(address);
        LOG.log(Level.FINE, MessageFormat.format("firstFoundIdx: {0}", firstFoundIdx));
        if (firstFoundIdx != Integer.MAX_VALUE) {
            LOG.log(Level.FINE, MessageFormat.format("firstFoundIdx-substring: {0}", recipients.substring(firstFoundIdx)));
            type = getType(firstFoundIdx);
            idx = firstFoundIdx + lengthOfTypePrefix(type) + address.toString().length()
                    + adaptLengthForOptionalPersonal(address) + 1;
        }
        return type;
    }

    private int findFirst(InternetAddress address) {
        int firstIdx = Integer.MAX_VALUE;
        if (address.getPersonal() != null) {
            firstIdx = findFirst(firstIdx, "bcc:" + address.getPersonal());
            firstIdx = findFirst(firstIdx, "bcc:\"" + address.toString() + "\"");
            firstIdx = findFirst(firstIdx, "cc:" + address.getPersonal());
            firstIdx = findFirst(firstIdx, "cc:\"" + address.toString() + "\"");
            firstIdx = findFirst(firstIdx, address.getPersonal());
            firstIdx = findFirst(firstIdx, "\"" + address.toString() + "\"");
        } else {
            firstIdx = findFirst(firstIdx, "bcc:" + address.toString());
            firstIdx = findFirst(firstIdx, "cc:" + address.toString());
            firstIdx = findFirst(firstIdx, address.toString());
        }
        return firstIdx;
    }

    private int findFirst(int firstIdx, String search) {
        int foundIdx = recipients.indexOf(search, idx);
        return (foundIdx == -1) ? firstIdx : Math.min(firstIdx, foundIdx);
    }

    private int getType(int firstFoundIdx) {
        int type;
        if (recipients.indexOf("bcc", firstFoundIdx) == firstFoundIdx) {
            type = EmailRecipientUtils.BCC;
        } else if (recipients.indexOf("cc", firstFoundIdx) == firstFoundIdx) {
            type = EmailRecipientUtils.CC;
        } else {
            type = EmailRecipientUtils.TO;
        }
        return type;
    }

    /**
     * Mind: Include ':' suffix, e.g. for "bcc:<email address>" it is "bcc" + ":", i.e. 3 + 1 = 4
     */
    private int lengthOfTypePrefix(int type) {
        int length;
        switch (type) {
            case EmailRecipientUtils.BCC:
                length = 4;
                break;
            case EmailRecipientUtils.CC:
                length = 3;
                break;
            case EmailRecipientUtils.TO:
                length = 0;
                break;
            default:
                throw new IllegalArgumentException("Unknown type prefix: " + type);
        }
        return length;
    }

    private int adaptLengthForOptionalPersonal(InternetAddress address) {
        return address.getPersonal() != null ? -4 : 0; // This may not be precise! Quotes around personal name + '<' and '>' around email address...
    }

}
