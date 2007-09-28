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
	 * Specifies whether or not we should send this email to the developer/s who made changes
	 */
	private boolean sendToDevelopers;
	
	/**
	 * A comma-separated list of names of the recipientLists that we should send emails to 
	 */
	private boolean sendToRecipientList;
	
	public EmailType(){
		subject = "";
		body = "";
		recipientList = "";
		sendToDevelopers = false;
		sendToRecipientList = false;
	}

	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}

	public boolean getSendToDevelopers() {
		return sendToDevelopers;
	}

	public void setSendToDevelopers(boolean sendToDevelopers) {
		this.sendToDevelopers = sendToDevelopers;
	}

	public boolean getSendToRecipientList() {
		return sendToRecipientList;
	}

	public void setSendToRecipientList(boolean sendToRecipientList) {
		this.sendToRecipientList = sendToRecipientList;
	}

	public boolean getHasRecipients(){
		if(!sendToRecipientList && !sendToDevelopers && (recipientList==null || recipientList.trim().length()==0))
			return false;
		
		return true;
	}

	public String getRecipientList() {
		return recipientList;
	}

	public void setRecipientList(String recipientList) {
		this.recipientList = recipientList;
	}
}
