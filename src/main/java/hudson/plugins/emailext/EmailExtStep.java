package hudson.plugins.emailext;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.trigger.AlwaysTrigger;
import jenkins.model.Jenkins;
import hudson.Util;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.management.Descriptor;

/**
 * Created by acearl on 9/14/2015.
 */

public class EmailExtStep extends AbstractStepImpl {

    public final String subject;

    public final String body;

    @CheckForNull
    private String attachmentsPattern;

    @CheckForNull
    private String to;

    @CheckForNull
    private String replyTo;

    @CheckForNull
    private String mimeType;

    private boolean attachLog;

    private boolean compressLog;

    @DataBoundConstructor
    public EmailExtStep(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public @CheckForNull String getAttachmentsPattern() {
        return attachmentsPattern == null ? "" : attachmentsPattern;
    }

    @DataBoundSetter
    public void setAttachmentsPattern(@CheckForNull String attachmentsPattern) {
        if (StringUtils.isNotBlank(attachmentsPattern)) {
            this.attachmentsPattern = attachmentsPattern;
        }
    }

    public @CheckForNull String getTo() {
        return to == null ? "" : to;
    }

    @DataBoundSetter
    public void setTo(@CheckForNull String to) {
        this.to = Util.fixNull(to);
    }

    public @CheckForNull String getReplyTo() {
        return replyTo == null ? "" : replyTo;
    }

    @DataBoundSetter
    public void setReplyTo(@CheckForNull String replyTo) {
        this.replyTo = Util.fixNull(replyTo);
    }

    public @CheckForNull String getMimeType() {
        return mimeType == null ? "" : mimeType;
    }

    @DataBoundSetter
    public void setMimeType(@CheckForNull String mimeType) {
        this.mimeType = Util.fixNull(mimeType);
    }

    public boolean isAttachLog() {
        return attachLog;
    }

    @DataBoundSetter
    public void setAttachLog(boolean attachLog) {
        this.attachLog = attachLog;
    }

    public boolean isCompressLog() {
        return compressLog;
    }

    @DataBoundSetter
    public void setCompressLog(boolean compressLog) {
        this.compressLog = compressLog;
    }

    public static class EmailExtStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient EmailExtStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient Run<?,?> run;

        @StepContextParameter
        private transient FilePath workspace;

        @Override
        protected Void run() throws Exception {
            ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
            publisher.configuredTriggers.clear();

            AlwaysTrigger.DescriptorImpl descriptor = Jenkins.getActiveInstance().getDescriptorByType(AlwaysTrigger.DescriptorImpl.class);
            publisher.configuredTriggers.add(descriptor.createDefault());

            publisher.defaultSubject = step.subject;
            publisher.defaultContent = step.body;
            publisher.attachBuildLog = step.attachLog;
            publisher.compressBuildLog = step.compressLog;

            if (StringUtils.isNotBlank(step.to)) {
                publisher.recipientList = step.to;
            }

            if (StringUtils.isNotBlank(step.replyTo)) {
                publisher.replyTo = step.replyTo;
            }

            if (StringUtils.isNotBlank(step.attachmentsPattern)) {
                publisher.attachmentsPattern = step.attachmentsPattern;
            }

            if (StringUtils.isNotBlank(step.mimeType)) {
                publisher.contentType = step.mimeType;
            }

            final ExtendedEmailPublisherContext ctx = new ExtendedEmailPublisherContext(publisher, run, workspace, launcher, listener);
            final Multimap<String, EmailTrigger> triggered = ArrayListMultimap.create();
            triggered.put(AlwaysTrigger.TRIGGER_NAME, publisher.configuredTriggers.get(0));
            ctx.setTrigger(publisher.configuredTriggers.get(0));
            ctx.setTriggered(triggered);
            publisher.sendMail(ctx);
            return null;
        }
    }


    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public static final String defaultMimeType = "text/plain";

        public DescriptorImpl() {
            super(EmailExtStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "emailext";
        }

        @Override
        public String getDisplayName() {
            return "Extended Email";
        }
    }
}
