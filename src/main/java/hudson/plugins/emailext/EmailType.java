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

    /**
     * Pattern for attachments to be sent as part of this email type.
     */
    private String attachmentsPattern;

    /**
     * True to attach the build log to the email
     */
    private boolean attachBuildLog;

    /**
     * True to compress the build log before attaching it to the email
     */
    private boolean compressBuildLog;

    /**
     * List of email addresses to put into the Reply-To header
     */
    private String replyTo;
    
    /**
     * Content type to send the email as (HTML or Plaintext)
     */
    private String contentType;

    public EmailType() {
        subject = "";
        body = "";
        recipientList = "";
        sendToDevelopers = false;
        includeCulprits = false;
        sendToRecipientList = false;
        sendToRequester = false;
        attachmentsPattern = "";
        attachBuildLog = false;
        compressBuildLog = false;
        replyTo = "";
        contentType = "project";
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

    public boolean getSendToRequester() {
        return sendToRequester;
    }

    public void setSendToRequester(boolean sendToRequester) {
        this.sendToRequester = sendToRequester;
    }

    public boolean getSendToCulprits() {
        return includeCulprits;
    }

    public void setSendToCulprits(boolean sendToCulprits) {
        this.includeCulprits = sendToCulprits;
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

    public String getReplyTo() {
        return replyTo != null ? replyTo.trim() : replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo.trim();
    }

    public String getAttachmentsPattern() {
        return attachmentsPattern != null ? attachmentsPattern.trim() : attachmentsPattern;
    }

    public void setAttachmentsPattern(String attachmentsPattern) {
        this.attachmentsPattern = attachmentsPattern;
    }

    public boolean getAttachBuildLog() {
        return attachBuildLog;
    }

    public boolean getCompressBuildLog() {
        return compressBuildLog;
    }

    public void setAttachBuildLog(boolean attachBuildLog) {
        this.attachBuildLog = attachBuildLog;
    }

    public void setCompressBuildLog(boolean compressBuildLog) {
        this.compressBuildLog = compressBuildLog;
    }
    
    public String getContentType() {
        if(contentType == null)
            contentType = "project";
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Object readResolve() {
        if(this.recipientList != null) {
            // get rid of PROJECT_DEFAULT_RECIPIENTS stuff
            this.recipientList = this.recipientList.replaceAll("\\$\\{?PROJECT_DEFAULT_RECIPIENTS\\}?", ""); 
        }
        return this;
    }
}
