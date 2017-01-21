package hudson.plugins.emailext;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.inject.Inject;
import javax.mail.internet.InternetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EmailExtRecipientStep extends AbstractStepImpl {
    private List<RecipientProvider> recipientProviders;

    @DataBoundConstructor
    public EmailExtRecipientStep(List<RecipientProvider> recipientProviders) {
        this.recipientProviders = recipientProviders;
    }

    public List<RecipientProvider> getRecipientProviders() {
        return recipientProviders;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Executor extends AbstractSynchronousNonBlockingStepExecution<String> {
        private static final long serialVersionUID = 1L;

        @Inject
        private transient EmailExtRecipientStep step;

        @StepContextParameter
        private transient Run<?, ?> run;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected String run() throws Exception {
            if (step.recipientProviders == null || step.recipientProviders.isEmpty()) {
                throw new IllegalArgumentException("You must provide at least one recipient provider");
            }
            ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
            ExtendedEmailPublisherContext context = new ExtendedEmailPublisherContext(publisher, run, null, null, listener);
            Set<InternetAddress> to = new HashSet<>();
            RecipientProvider.checkAllSupport(step.recipientProviders, run.getParent().getClass());
            for (RecipientProvider provider : step.recipientProviders) {
                provider.addRecipients(context, env, to, to, to);
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
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Executor.class);
        }

        @Override
        public String getFunctionName() {
            return "emailextrecipients";
        }

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
