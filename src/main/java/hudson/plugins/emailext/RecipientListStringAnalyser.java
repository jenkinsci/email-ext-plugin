package hudson.plugins.emailext;

import hudson.model.TaskListener;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;

class RecipientListStringAnalyser {

    static final int NOT_FOUND = -1;

    // Needed for debug logging according to configure system settings; both may be null
    private final TaskListener listener;
    private final ExtendedEmailPublisherDescriptor descriptor;

    private final String recipients;

    private int idx = 0;

    /**
     * Only for testing.
     */
    RecipientListStringAnalyser(String recipientsListString) {
        this(null, null, recipientsListString);
    }

    private RecipientListStringAnalyser(TaskListener listener, ExtendedEmailPublisherDescriptor descriptor,
                                        String recipientsListString) {
        this.listener = listener;
        this.descriptor = descriptor;
        this.recipients = recipientsListString;

        debug("Analyzing: %s", recipients);
    }

    static RecipientListStringAnalyser newInstance(TaskListener listener, String recipientsListString) {
        ExtendedEmailPublisherDescriptor descriptor = Jenkins.get().getDescriptorByType(
                ExtendedEmailPublisherDescriptor.class);
        return new RecipientListStringAnalyser(listener, descriptor, recipientsListString);
    }

    /**
     * Mind that this method must be called in order of the found InternetAddresses (from parsing the recipients list
     * string)!
     */
    int getType(InternetAddress address) {
        int type = NOT_FOUND;
        debug("Looking for: %s", address);
        debug("\tstarting at: %d", idx);
        int firstFoundIdx = findFirst(address);
        debug("\tfirstFoundIdx: %d", firstFoundIdx);
        if (firstFoundIdx != Integer.MAX_VALUE) {
            debug("\tfirstFoundIdx-substring: %s", recipients.substring(firstFoundIdx));
            type = getType(firstFoundIdx);
            debug("\t=> found type: %d", type);
            idx = firstFoundIdx + lengthOfTypePrefix(type) + address.toString().length()
                    + adaptLengthForOptionalPersonal(address) + 1;
        } else {
            debug("\t=> type not found");
        }
        return type;
    }

    private int findFirst(InternetAddress address) {
        int firstIdx = Integer.MAX_VALUE;
        if (address.getPersonal() != null) {
            firstIdx = findFirst(firstIdx, "bcc:" + address.getPersonal());
            firstIdx = findFirst(firstIdx, "bcc:\"" + address + "\"");
            firstIdx = findFirst(firstIdx, "cc:" + address.getPersonal());
            firstIdx = findFirst(firstIdx, "cc:\"" + address + "\"");
            firstIdx = findFirst(firstIdx, address.getPersonal());
            firstIdx = findFirst(firstIdx, "\"" + address + "\"");
        } else {
            firstIdx = findFirst(firstIdx, "bcc:" + address);
            firstIdx = findFirst(firstIdx, "cc:" + address);
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
        if (recipients.indexOf("bcc:", firstFoundIdx) == firstFoundIdx) {
            type = EmailRecipientUtils.BCC;
        } else if (recipients.indexOf("cc:", firstFoundIdx) == firstFoundIdx) {
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

    private void debug(final String format, final Object... args) {
        if (descriptor != null && listener != null) {
            descriptor.debug(listener.getLogger(), format, args);
        }
    }

}
