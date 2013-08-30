package hudson.plugins.emailext.plugins;

import hudson.ExtensionPoint;
import hudson.DescriptorExtensionList;
import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.TaskListener;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;

import jenkins.model.Jenkins;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public abstract class EmailTrigger implements Describable<EmailTrigger>, ExtensionPoint {

    private EmailType email;

    public static DescriptorExtensionList<EmailTrigger, EmailTriggerDescriptor> all() {
        return Jenkins.getInstance().<EmailTrigger, EmailTriggerDescriptor>getDescriptorList(EmailTrigger.class);
    }

    protected EmailTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, String recipientList,
            String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        email = new EmailType();
        email.setSendToRecipientList(sendToList);
        email.setSendToDevelopers(sendToDevs);
        email.setSendToRequester(sendToRequestor);
        email.setRecipientList(recipientList);
        email.setReplyTo(replyTo);
        email.setSubject(subject);
        email.setBody(body);
        email.setAttachmentsPattern(attachmentsPattern);
        email.setAttachBuildLog(attachBuildLog > 0);
        email.setCompressBuildLog(attachBuildLog > 1);
        email.setContentType(contentType);
        email.setSendToCulprits(sendToCulprits);
    }
    
    protected EmailTrigger(JSONObject formData) {
        
    }

    /**
     * Implementors of this method need to return true if the conditions to
     * trigger an email have been met.
     *
     * @param build The Build object after the project has been built
     * @return true if the conditions have been met to trigger a build of this
     * type
     */
    public abstract boolean trigger(AbstractBuild<?, ?> build, TaskListener listener);

    /**
     * Get the email that is with this trigger.
     *
     * @return the email
     */
    public EmailType getEmail() {
        return email;
    }

    public void setEmail(EmailType email) {
        if (email == null) {
            email = new EmailType();
            email.setBody(ExtendedEmailPublisher.PROJECT_DEFAULT_BODY_TEXT);
            email.setSubject(ExtendedEmailPublisher.PROJECT_DEFAULT_SUBJECT_TEXT);
        }
        this.email = email;
    }

    public EmailTriggerDescriptor getDescriptor() {
        return (EmailTriggerDescriptor) Jenkins.getInstance().getDescriptor(getClass());
    }

    public boolean configure(StaplerRequest req, JSONObject formData) {
        setEmail(createMailType(formData));
        return true;
    }

    /**
     * Determine the number of direct failures in the given build. If it
     * aggregates downstream results, ignore contributed failures. This is
     * because at the time this trigger runs, the current build's aggregated
     * results aren't available yet, but those of the previous build may be.
     */
    protected int getNumFailures(AbstractBuild<?, ?> build) {
        AbstractTestResultAction a = build.getTestResultAction();
        if (a instanceof AggregatedTestResultAction) {
            int result = 0;
            AggregatedTestResultAction action = (AggregatedTestResultAction) a;
            for (ChildReport cr : action.getChildReports()) {
                if (cr == null || cr.child == null || cr.child.getParent() == null) {
                    continue;
                }
                if (cr.child.getParent().equals(build.getParent())) {
                    if (cr.result instanceof TestResult) {
                        TestResult tr = (TestResult) cr.result;
                        result += tr.getFailCount();
                    }
                }
            }

            if (result == 0 && action.getFailCount() > 0) {
                result = action.getFailCount();
            }
            return result;
        }
        return a.getFailCount();
    }

    /**
     * Should this trigger run before the build? Defaults to false.
     */
    public boolean isPreBuild() {
        return false;
    }

    protected EmailType createMailType(JSONObject formData) {
        EmailType m = new EmailType();
        String prefix = "mailer_" + getDescriptor().getJsonSafeClassName() + '_';
        m.setSubject(formData.getString(prefix + "subject"));
        m.setBody(formData.getString(prefix + "body"));
        m.setRecipientList(formData.getString(prefix + "recipientList"));
        m.setSendToRecipientList(formData.optBoolean(prefix + "sendToRecipientList"));
        m.setSendToDevelopers(formData.optBoolean(prefix + "sendToDevelopers"));
        m.setSendToRequester(formData.optBoolean(prefix + "sendToRequester"));
        m.setSendToCulprits(formData.optBoolean(prefix + "sendToCulprits"));
        m.setAttachmentsPattern(formData.getString(prefix + "attachmentsPattern"));
        m.setAttachBuildLog(formData.optBoolean(prefix + "attachBuildLog"));
        m.setReplyTo(formData.getString(prefix + "replyTo"));
        m.setContentType(formData.getString(prefix + "contentType"));
        return m;
    }
}
