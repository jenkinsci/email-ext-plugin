package hudson.plugins.emailext;

/**
 * This class defines what the contents of an email will be if it gets sent.
 * 
 * @author kyle.sweeney@valtech.com
 */
public class EmailType {

    /**
     * A recipient list for only this email type.
     */
    private String recipientList;

    /**
     * The subject of the email
     */
    private String subject;

    /**
     * The body of the email
     */
    private String body;

    /**
     * Specifies whether or not we should send this email to the developer/s
     * who made changes.
     */
    private boolean sendToDevelopers;

    /**
     * Specifies whether or not we should send this email to the requester
     * who triggered build.
     */
    private boolean sendToRequester;

    /**
     * Specifies whether or not we should send this email to all developers
     * since the last success.
     */
    private boolean includeCulprits;

    /**
     * Specifies whether or not we should send this email to the recipient list
     */
    private boolean sendToRecipientList;

    public EmailType() {
        subject = "";
        body = "";
        recipientList = "";
        sendToDevelopers = false;
        includeCulprits = false;
        sendToRecipientList = false;
        sendToRequester = false;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean getSendToDevelopers() {
        return sendToDevelopers;
    }

    public void setSendToDevelopers(boolean sendToDevelopers) {
        this.sendToDevelopers = sendToDevelopers;
    }

    public boolean isSendToRequester() {
        return sendToRequester;
    }

    public void setSendToRequester(boolean sendToRequester) {
        this.sendToRequester = sendToRequester;
    }

    public boolean getIncludeCulprits() {
        return includeCulprits;
    }

    public void setIncludeCulprits(boolean includeCulprits) {
        this.includeCulprits = includeCulprits;
    }

    public boolean getSendToRecipientList() {
        return sendToRecipientList;
    }

    public void setSendToRecipientList(boolean sendToRecipientList) {
        this.sendToRecipientList = sendToRecipientList;
    }

    public boolean getHasRecipients() {
        return sendToRecipientList
                || sendToDevelopers
                || (recipientList != null && recipientList.trim().length() != 0);
    }

    public String getRecipientList() {
        return recipientList != null ? recipientList.trim() : recipientList;
    }

    public void setRecipientList(String recipientList) {
        this.recipientList = recipientList.trim();
    }

    public Object readResolve() {
        if(this.recipientList != null) {
            // get rid of PROJECT_DEFAULT_RECIPIENTS stuff
            this.recipientList = this.recipientList.replaceAll("\\$\\{?PROJECT_DEFAULT_RECIPIENTS\\}?", ""); 
        }
        return this;
    }
}
