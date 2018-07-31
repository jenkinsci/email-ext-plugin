package hudson.plugins.emailext;

import javax.mail.internet.InternetAddress;

class RecipientListStringAnalyser {

    static final int NOT_FOUND = -1;

    private final String recipients;

    private int idx = 0;

    RecipientListStringAnalyser(String recipientsListString) {
        this.recipients = recipientsListString;
        System.out.println("Analyzing:");
        System.out.println(recipients);
    }

    /**
     * Mind that this method must be called in order of the found InternetAddresses (from parsing the recipients list
     * string)!
     */
    int getType(InternetAddress address) {
        int type = NOT_FOUND;
        System.out.println("Looking for: " + address);
        System.out.println("...starting at: " + idx);
        int firstFoundIdx = findFirst(address);
        System.out.println("firstFoundIdx: " + firstFoundIdx);
        if (firstFoundIdx != Integer.MAX_VALUE) {
            System.out.println("firstFoundIdx-substring: " + recipients.substring(firstFoundIdx));
            type = getType(firstFoundIdx);
            idx += address.toString().length() + 1 + lengthOfTypePrefix(type) + adaptLengthForOptionalPersonal(address);
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

    private int lengthOfTypePrefix(int type) {
        int length;
        switch (type) {
            case EmailRecipientUtils.BCC:
                length = 3;
                break;
            case EmailRecipientUtils.CC:
                length = 2;
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
