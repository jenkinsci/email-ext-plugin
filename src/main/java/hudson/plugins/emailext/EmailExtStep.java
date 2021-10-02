package hudson.plugins.emailext;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.trigger.AlwaysTrigger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Created by acearl on 9/14/2015.
 */

public class EmailExtStep extends Step {

    public final String subject;

    public final String body;

    @CheckForNull
    private String attachmentsPattern;

    @CheckForNull
    private String to;

    @CheckForNull
    private String replyTo;
    
    @CheckForNull
    private String from;

    @CheckForNull
    private String mimeType;

    private boolean attachLog;

    private boolean compressLog;

    private List<RecipientProvider> recipientProviders;

    private String presendScript;

    private String postsendScript;

    private boolean saveOutput;

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

    public @CheckForNull String getFrom() {
        return from == null ? "" : from;
    }

    @DataBoundSetter
    public void setFrom(@CheckForNull String from) {
        this.from = Util.fixNull(from);
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

    public boolean getAttachLog() {
        return attachLog;
    }

    @DataBoundSetter
    public void setAttachLog(boolean attachLog) {
        this.attachLog = attachLog;
    }

    public boolean getCompressLog() {
        return compressLog;
    }

    @DataBoundSetter
    public void setCompressLog(boolean compressLog) {
        this.compressLog = compressLog;
    }

    @DataBoundSetter
    public void setRecipientProviders(List<RecipientProvider> recipientProviders) {
        this.recipientProviders = recipientProviders;
    }

    public List<? extends RecipientProvider> getRecipientProviders() {
        return recipientProviders;
    }

    @DataBoundSetter
    public void setPresendScript(String presendScript) {
        this.presendScript = presendScript;
    }

    public String getPresendScript() {
        return presendScript == null ? "" : presendScript;
    }

    @DataBoundSetter
    public void setPostsendScript(String postsendScript) {
        this.postsendScript = postsendScript;
    }

    public String getPostsendScript() {
        return postsendScript == null ? "" : postsendScript;
    }

    public boolean getSaveOutput() {
        return saveOutput;
    }

    @DataBoundSetter
    public void setSaveOutput(boolean saveOutput) {
        this.saveOutput = saveOutput;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new EmailExtStepExecution(this, context);
    }

    public static class EmailExtStepExecution extends SynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        private final transient EmailExtStep step;

        protected EmailExtStepExecution(EmailExtStep step, @NonNull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
            publisher.configuredTriggers.clear();

            AlwaysTrigger.DescriptorImpl descriptor = Jenkins.get().getDescriptorByType(AlwaysTrigger.DescriptorImpl.class);
            EmailTrigger trigger = descriptor.createDefault();
            trigger.getEmail().getRecipientProviders().clear();
            trigger.getEmail().addRecipientProvider(new ListRecipientProvider());
            if (step.recipientProviders != null) {
                RecipientProvider.checkAllSupport(
                        step.recipientProviders,
                        getContext().get(Run.class).getParent().getClass());
                trigger.getEmail().addRecipientProviders(step.recipientProviders);
            }
            publisher.configuredTriggers.add(trigger);

            publisher.saveOutput = step.saveOutput;
            publisher.defaultSubject = step.subject;
            publisher.defaultContent = step.body;
            publisher.attachBuildLog = step.attachLog;
            publisher.compressBuildLog = step.compressLog;
            publisher.setPresendScript(step.presendScript);
            publisher.setPostsendScript(step.postsendScript);

            if (StringUtils.isNotBlank(step.to)) {
                publisher.recipientList = step.to;
            }

            if (StringUtils.isNotBlank(step.replyTo)) {
                publisher.replyTo = step.replyTo;
            }
            
            if (StringUtils.isNotBlank(step.from)) {
                publisher.from = step.from;
            }
            
            if (StringUtils.isNotBlank(step.attachmentsPattern)) {
                publisher.attachmentsPattern = step.attachmentsPattern;
            }

            if (StringUtils.isNotBlank(step.mimeType)) {
                publisher.contentType = step.mimeType;
            }

            final ExtendedEmailPublisherContext ctx =
                    new ExtendedEmailPublisherContext(
                            publisher,
                            getContext().get(Run.class),
                            getContext().get(FilePath.class),
                            getContext().get(Launcher.class),
                            getContext().get(TaskListener.class));
            final Multimap<String, EmailTrigger> triggered = ArrayListMultimap.create();
            triggered.put(AlwaysTrigger.TRIGGER_NAME, publisher.configuredTriggers.get(0));
            ctx.setTrigger(publisher.configuredTriggers.get(0));
            ctx.setTriggered(triggered);
            publisher.sendMail(ctx);
            return null;
        }
    }


    @Extension(optional=true)
    public static final class DescriptorImpl extends StepDescriptor {

        public static final String defaultMimeType = "text/plain";

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, Run.class, TaskListener.class);
            return Collections.unmodifiableSet(context);
        }

        @Override
        public String getFunctionName() {
            return "emailext";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Extended Email";
        }

        @SuppressWarnings("unused")
        public List<RecipientProviderDescriptor> getRecipientProvidersDescriptors() {
            return RecipientProvider.allSupporting(WorkflowJob.class);
        }
    }
}
