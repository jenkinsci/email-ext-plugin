package hudson.plugins.emailext.plugins.recipients;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.Messages;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.scm.ChangeLogSet;
import jakarta.mail.internet.InternetAddress;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class UpstreamComitterSinceLastSuccessRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public UpstreamComitterSinceLastSuccessRecipientProvider() {}

    @Override
    public void addRecipients(
            final ExtendedEmailPublisherContext context,
            EnvVars env,
            Set<InternetAddress> to,
            Set<InternetAddress> cc,
            Set<InternetAddress> bcc) {
        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor =
                    Jenkins.get().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            @Override
            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }

        final Debug debug = new Debug();
        Run<?, ?> currentBuild = context.getRun();
        Run<?, ?> lastSuccessfulBuild = currentBuild.getPreviousSuccessfulBuild();
        if (lastSuccessfulBuild == null) {
            debug.send(
                    "No previous successful build for job %s#%s, skipping upstream committers since last success.",
                    currentBuild.getParent().getName(), currentBuild.getNumber());
            return; // No previous successful build, so we can't determine upstream committers since last success
        }
        debug.send("Sending email to upstream committer(s) since last successful build.");
        debug.send(
                "Collecting upstream builds for job %s#%s since last success (%s#%s).",
                currentBuild.getParent().getName(),
                currentBuild.getNumber(),
                lastSuccessfulBuild.getParent().getName(),
                lastSuccessfulBuild.getNumber());

        // Walk back through all curr job builds since last success and collect all upstream SCM builds that triggered
        // them
        Set<Run<?, ?>> upstreamBuilds = new HashSet<>();
        Run<?, ?> jobBuild = currentBuild;

        while (jobBuild != null && jobBuild != lastSuccessfulBuild) {
            for (Cause c : jobBuild.getCauses()) {
                if (c instanceof Cause.UpstreamCause cause) {
                    collectUpstreamBuilds(cause, upstreamBuilds);
                }
            }
            jobBuild = jobBuild.getPreviousBuild();
        }

        debug.send("Found %d upstream builds in the time window.", upstreamBuilds.size());
        for (Run<?, ?> run : upstreamBuilds) {
            addUpstreamCommittersTriggeringBuild(run, to, cc, bcc, env, context, debug);
        }
    }

    // Recursively collect all upstream runs across the full dependency tree
    private static void collectUpstreamBuilds(Cause.UpstreamCause cause, Set<Run<?, ?>> result) {
        Run<?, ?> r = cause.getUpstreamRun();
        if (r != null) {
            result.add(r);
            for (Cause c : cause.getUpstreamCauses()) {
                if (c instanceof Cause.UpstreamCause upstream) {
                    collectUpstreamBuilds(upstream, result);
                }
            }
        }
    }

    /**
     * Adds for the given upstream build the committers to the recipient list for
     * each commit in the upstream build.
     *
     * @param run the upstream build
     * @param to  the to recipient list
     * @param cc  the cc recipient list
     * @param bcc the bcc recipient list
     * @param env the build environment
     */
    private void addUpstreamCommittersTriggeringBuild(
            Run<?, ?> run,
            Set<InternetAddress> to,
            Set<InternetAddress> cc,
            Set<InternetAddress> bcc,
            EnvVars env,
            final ExtendedEmailPublisherContext context,
            RecipientProviderUtilities.IDebug debug) {
        debug.send(
                "Adding upstream committer from job %s with build number %s",
                run.getParent().getDisplayName(), run.getNumber());
        if (run instanceof RunWithSCM<?, ?> cM) {
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = cM.getChangeSets();

            for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
                for (ChangeLogSet.Entry change : changeSet) {
                    addUserFromChangeSet(change, to, cc, bcc, env, context, debug);
                }
            }
        }
    }

    /**
     * Adds a user to the recipients list based on a specific SCM change set
     *
     * @param change The ChangeLogSet.Entry to get the user information from
     * @param to     The list of to addresses to add to
     * @param cc     The list of cc addresses to add to
     * @param bcc    The list of bcc addresses to add to
     * @param env    The build environment
     */
    private void addUserFromChangeSet(
            ChangeLogSet.Entry change,
            Set<InternetAddress> to,
            Set<InternetAddress> cc,
            Set<InternetAddress> bcc,
            EnvVars env,
            final ExtendedEmailPublisherContext context,
            RecipientProviderUtilities.IDebug debug) {
        User user = change.getAuthor();
        RecipientProviderUtilities.addUsers(Collections.singleton(user), context, env, to, cc, bcc, debug);
    }

    @Extension
    @Symbol("upstreamDevelopersSinceLastSuccess")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.UpstreamComitterSinceLastSuccessRecipientProvider_DisplayName();
        }
    }
}
