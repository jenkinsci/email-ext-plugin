package hudson.plugins.emailext;

import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.CulpritsRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.RequesterRecipientProvider;
import java.util.ArrayList;
import java.util.List;

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
     * The list of configured recipient providers
     */
    private List<RecipientProvider> recipientProviders;

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

    /**
     * Specifies whether or not we should send this email to the developer/s who
     * made changes.
     */
    private transient boolean sendToDevelopers;

    /**
     * Specifies whether or not we should send this email to the requester who
     * triggered build.
     */
    private transient boolean sendToRequester;

    /**
     * Specifies whether or not we should send this email to all developers
     * since the last success.
     */
    private transient boolean includeCulprits;

    /**
     * Specifies whether or not we should send this email to the recipient list
     */
    private transient boolean sendToRecipientList;

    public EmailType() {
        subject = "";
        body = "";
        recipientList = "";
        attachmentsPattern = "";
        attachBuildLog = false;
        compressBuildLog = false;
        replyTo = "";
        contentType = "project";
        recipientProviders = new ArrayList<RecipientProvider>();
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

    public boolean getHasRecipients() {
        return (recipientProviders != null && !recipientProviders.isEmpty())
                || (recipientList != null && recipientList.trim().length() != 0);
    }

    public String getRecipientList() {
        return recipientList != null ? recipientList.trim() : recipientList;
    }

    public List<RecipientProvider> getRecipientProviders() {
        return recipientProviders;
    }

    public void addRecipientProvider(RecipientProvider provider) {
        if (recipientProviders == null) {
            recipientProviders = new ArrayList<RecipientProvider>();
        }
        recipientProviders.add(provider);
    }

    public void addRecipientProviders(List<RecipientProvider> providers) {
        if (recipientProviders == null) {
            recipientProviders = new ArrayList<RecipientProvider>();
        }
        if(providers != null)
            recipientProviders.addAll(providers);
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
        if (contentType == null) {
            contentType = "project";
        }
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Object readResolve() {
        if (this.recipientList != null) {
            // get rid of PROJECT_DEFAULT_RECIPIENTS stuff
            this.recipientList = this.recipientList.replaceAll("\\$\\{?PROJECT_DEFAULT_RECIPIENTS\\}?", "");
        }

        // upgrade the various fields to the new RecipientProvider method
        if (this.sendToDevelopers) {
            if (recipientProviders == null) {
                recipientProviders = new ArrayList<RecipientProvider>();
            }

            recipientProviders.add(new DevelopersRecipientProvider());
        }

        if (this.sendToRequester) {
            if (recipientProviders == null) {
                recipientProviders = new ArrayList<RecipientProvider>();
            }

            recipientProviders.add(new RequesterRecipientProvider());
        }

        if (this.includeCulprits) {
            if (recipientProviders == null) {
                recipientProviders = new ArrayList<RecipientProvider>();
            }

            recipientProviders.add(new CulpritsRecipientProvider());
        }

        if (this.sendToRecipientList) {
            if (recipientProviders == null) {
                recipientProviders = new ArrayList<RecipientProvider>();
            }

            recipientProviders.add(new ListRecipientProvider());
        }

        return this;
    }
    
    @Deprecated
    public boolean getSendToCulprits() {
        for(RecipientProvider p : recipientProviders) {
            if(p instanceof CulpritsRecipientProvider) {
                return true;
            }
        }
        return false;
    }
    
    @Deprecated
    public void setSendToCulprits(boolean sendToCulprits) {
        if(sendToCulprits && !getSendToCulprits()) {
            // need to add
            recipientProviders.add(new CulpritsRecipientProvider());
        } else if(!sendToCulprits && getSendToCulprits()) {
            int index;
            for(index = 0; index < recipientProviders.size(); index++) {
                if(recipientProviders.get(index) instanceof CulpritsRecipientProvider) {
                    break;
                }
            }
            
            if(index >=0 && index < recipientProviders.size()) {
                recipientProviders.remove(index);
            }
        }
    }
    
    @Deprecated
    public boolean getSendToDevelopers() {
        for(RecipientProvider p : recipientProviders) {
            if(p instanceof DevelopersRecipientProvider) {
                return true;
            }
        }
        return false;
    }
    
    @Deprecated
    public void setSendToDevelopers(boolean sendToDevelopers) {
        if(sendToDevelopers && !getSendToDevelopers()) {
            // need to add
            recipientProviders.add(new DevelopersRecipientProvider());
        } else if(!sendToDevelopers && getSendToDevelopers()) {
            int index;
            for(index = 0; index < recipientProviders.size(); index++) {
                if(recipientProviders.get(index) instanceof DevelopersRecipientProvider) {
                    break;
                }
            }
            
            if(index >= 0 && index < recipientProviders.size()) {
                recipientProviders.remove(index);
            }
        }
    }
    
    @Deprecated
    public boolean getSendToRequester() {
        for(RecipientProvider p : recipientProviders) {
            if(p instanceof RequesterRecipientProvider) {
                return true;
            }
        }
        return false;
    }
    
    @Deprecated
    public void setSendToRequester(boolean sendToRequester) {
        if(sendToRequester && !getSendToRequester()) {
            // need to add
            recipientProviders.add(new RequesterRecipientProvider());
        } else if(!sendToRequester && getSendToRequester()) {
            int index;
            for(index = 0; index < recipientProviders.size(); index++) {
                if(recipientProviders.get(index) instanceof RequesterRecipientProvider) {
                    break;
                }
            }
            
            if(index >= 0 && index < recipientProviders.size()) {
                recipientProviders.remove(index);
            }
        }
    }
    
    @Deprecated
    public boolean getSendToRecipientList() {
        for(RecipientProvider p : recipientProviders) {
            if(p instanceof ListRecipientProvider) {
                return true;
            }
        }
        return false;
    }
    
    @Deprecated
    public void setSendToRecipientList(boolean sendToRecipientList) {
        if(sendToRecipientList && !getSendToRecipientList()) {
            // need to add
            recipientProviders.add(new RequesterRecipientProvider());
        } else if(!sendToRecipientList && getSendToRecipientList()) {
            int index;
            for(index = 0; index < recipientProviders.size(); index++) {
                if(recipientProviders.get(index) instanceof ListRecipientProvider) {
                    break;
                }
            }
            
            if(index >= 0 && index < recipientProviders.size()) {
                recipientProviders.remove(index);
            }
        }
    }
}
