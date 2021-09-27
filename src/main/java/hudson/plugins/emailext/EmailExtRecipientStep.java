package hudson.plugins.emailext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.mail.internet.InternetAddress;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class EmailExtRecipientStep extends Step {
    private List<RecipientProvider> recipientProviders;

    @DataBoundConstructor
    public EmailExtRecipientStep(List<RecipientProvider> recipientProviders) {
        this.recipientProviders = recipientProviders;
    }

    public List<RecipientProvider> getRecipientProviders() {
        return recipientProviders;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Executor(this, context);
    }

    public static class Executor extends SynchronousNonBlockingStepExecution<String> {
        private static final long serialVersionUID = 1L;

        private final transient EmailExtRecipientStep step;

        protected Executor(EmailExtRecipientStep step, @NonNull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            if (step.recipientProviders == null || step.recipientProviders.isEmpty()) {
                throw new IllegalArgumentException("You must provide at least one recipient provider");
            }
            ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
            ExtendedEmailPublisherContext context =
                    new ExtendedEmailPublisherContext(
                            publisher,
                            getContext().get(Run.class),
                            null,
                            null,
                            getContext().get(TaskListener.class));
            Set<InternetAddress> to = new HashSet<>();
            RecipientProvider.checkAllSupport(
                    step.recipientProviders, getContext().get(Run.class).getParent().getClass());
            for (RecipientProvider provider : step.recipientProviders) {
                provider.addRecipients(context, getContext().get(EnvVars.class), to, to, to);
            }

            StringBuilder rt = new StringBuilder();

            Iterator<InternetAddress> iterator = to.iterator();
            while (iterator.hasNext()) {
                rt.append(iterator.next().toString());
                if (iterator.hasNext()) {
                    rt.append(" ");
                }
            }

            return rt.toString();
        }
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, Run.class, TaskListener.class, EnvVars.class);
            return Collections.unmodifiableSet(context);
        }

        @Override
        public String getFunctionName() {
            return "emailextrecipients";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Extended Email Recipients";
        }

        @SuppressWarnings("unused")
        public List<RecipientProviderDescriptor> getRecipientProvidersDescriptors() {
            return RecipientProvider.allSupporting(WorkflowJob.class);
        }
    }
}
