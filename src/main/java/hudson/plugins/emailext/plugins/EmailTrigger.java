package hudson.plugins.emailext.plugins;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.recipients.CulpritsRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.RequesterRecipientProvider;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

public abstract class EmailTrigger implements Describable<EmailTrigger>, ExtensionPoint {

    private EmailType email;

    @Deprecated
    protected EmailTrigger(boolean sendToList, boolean sendToDevs, boolean sendToRequestor, boolean sendToCulprits, String recipientList, String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        List<RecipientProvider> providers = new ArrayList<>();
        if(sendToList) {
            providers.add(new ListRecipientProvider());
        }

        if(sendToDevs) {
            providers.add(new DevelopersRecipientProvider());
        }

        if(sendToRequestor) {
            providers.add(new RequesterRecipientProvider());
        }

        if(sendToCulprits) {
            providers.add(new CulpritsRecipientProvider());
        }

        email = new EmailType();
        email.addRecipientProviders(providers);
        email.setRecipientList(recipientList);
        email.setReplyTo(replyTo);
        email.setSubject(subject);
        email.setBody(body);
        email.setAttachmentsPattern(attachmentsPattern);
        email.setAttachBuildLog(attachBuildLog > 0);
        email.setCompressBuildLog(attachBuildLog > 1);
        email.setContentType(contentType);
    }

    protected EmailTrigger(List<RecipientProvider> recipientProviders, String recipientList, String replyTo,
                           String subject, String body, String attachmentsPattern, int attachBuildLog, String contentType) {
        email = new EmailType();
        email.addRecipientProviders(recipientProviders);
        email.setRecipientList(recipientList);
        email.setReplyTo(replyTo);
        email.setSubject(subject);
        email.setBody(body);
        email.setAttachmentsPattern(attachmentsPattern);
        email.setAttachBuildLog(attachBuildLog > 0);
        email.setCompressBuildLog(attachBuildLog > 1);
        email.setContentType(contentType);
    }

    protected EmailTrigger(JSONObject formData) {

    }

    public static DescriptorExtensionList<EmailTrigger, EmailTriggerDescriptor> all() {
        return Jenkins.get().getDescriptorList(EmailTrigger.class);
    }

    public static List<EmailTriggerDescriptor> allWatchable() {
        List<EmailTriggerDescriptor> list = new ArrayList<>();
        for(EmailTriggerDescriptor d : all()) {
            if(d.isWatchable()) {
                list.add(d);
            }
        }

        return list;
    }

    /**
     * Implementors of this method need to return true if the conditions to
     * trigger an email have been met.
     *
     * @param build The Build object after the project has been built
     * @param listener Used for logging to the build log
     * @return true if the conditions have been met to trigger a build of this
     * type
     */
    public abstract boolean trigger(AbstractBuild<?, ?> build, TaskListener listener);

    /**
     * Get the email that is with this trigger.
     *
     * @return the email
     */
    @Whitelisted
    public EmailType getEmail() {
        return email;
    }

    @Whitelisted
    public void setEmail(EmailType email) {
        if (email == null) {
            email = new EmailType();
            email.setBody(ExtendedEmailPublisher.PROJECT_DEFAULT_BODY_TEXT);
            email.setSubject(ExtendedEmailPublisher.PROJECT_DEFAULT_SUBJECT_TEXT);
        }
        this.email = email;
    }

    public EmailTriggerDescriptor getDescriptor() {
        return (EmailTriggerDescriptor) Jenkins.get().getDescriptor(getClass());
    }
    
    public boolean configure(StaplerRequest req, JSONObject formData) {
        setEmail(createMailType(req, formData));
        return true;
    }
    
    @Deprecated
    protected EmailType createMailType(JSONObject formData) {
        return createMailType(Stapler.getCurrentRequest(), formData);
    }
    
    protected EmailType createMailType(StaplerRequest req, JSONObject formData) {
        return req.bindJSON(EmailType.class, formData);
    }

    /**
     * Determine the number of direct failures in the given build. If it
     * aggregates downstream results, ignore contributed failures. This is
     * because at the time this trigger runs, the current build's aggregated
     * results aren't available yet, but those of the previous build may be.
     *
     * @param build The project run to get the number of test failures for.
     * @return The number of test failures for the Run
     */
    protected int getNumFailures(Run<?, ?> build) {
        AbstractTestResultAction<? extends AbstractTestResultAction<?>> a = build.getAction(AbstractTestResultAction.class);
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
     * @return true if the trigger should be checked before the build.
     */
    public boolean isPreBuild() {
        return false;
    }
}
