package hudson.plugins.emailext.plugins;

import hudson.model.AbstractBuild;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;

public abstract class EmailTrigger {

    private EmailType email;

    /**
     * Implementors of this method need to return true if the conditions to
     * trigger an email have been met.
     * 
     * @param build
     *            The Build object after the project has been built
     * @return true if the conditions have been met to trigger a build of this
     *         type
     */
    public abstract boolean trigger(AbstractBuild<?, ?> build);

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

    public abstract EmailTriggerDescriptor getDescriptor();

    public boolean getDefaultSendToList() {
        return false;
    }

    public boolean getDefaultSendToDevs() {
        return false;
    }

    public boolean getDefaultSendToRequester() {
        return false;
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
                if(cr == null || cr.child == null || cr.child.getParent() == null) continue;
                if (cr.child.getParent().equals(build.getParent())) {
                    if (cr.result instanceof TestResult) {
                        TestResult tr = (TestResult) cr.result;
                        result += tr.getFailCount();
                    }
                }
            }

            if(result == 0 && action.getFailCount() > 0) {
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
}
