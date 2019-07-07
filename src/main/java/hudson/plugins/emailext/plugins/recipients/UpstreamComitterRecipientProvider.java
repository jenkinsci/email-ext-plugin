package hudson.plugins.emailext.plugins.recipients;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.Messages;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.scm.ChangeLogSet;
import java.io.PrintStream;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.internet.InternetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Sends emails to committers of upstream builds which triggered this build.
 */
public class UpstreamComitterRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public UpstreamComitterRecipientProvider() {

    }

    @Override
    public void addRecipients(final ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor
                    = Jenkins.get().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }
        final Debug debug = new Debug();
        debug.send("Sending email to upstream committer(s).");
        Run<?, ?> cur;
        Cause.UpstreamCause upc = context.getRun().getCause(Cause.UpstreamCause.class);
        while (upc != null) {
            Job<?, ?> p = (Job<?, ?>) Jenkins.get().getItemByFullName(upc.getUpstreamProject());
            if(p == null) {
                context.getListener().getLogger().print("There is a break in the project linkage, could not retrieve upstream project information");
                break;
            }
            cur = p.getBuildByNumber(upc.getUpstreamBuild());
            upc = cur.getCause(Cause.UpstreamCause.class);
            addUpstreamCommittersTriggeringBuild(cur, to, cc, bcc, env, context, debug);
        }
    }

    /**
     * Adds for the given upstream build the committers to the recipient list for each commit in the upstream build.
     *
     * @param run the upstream build
     * @param to the to recipient list
     * @param cc the cc recipient list
     * @param bcc the bcc recipient list
     * @param env the build environment
     */
    private void addUpstreamCommittersTriggeringBuild(Run<?, ?> run, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc, EnvVars env, final ExtendedEmailPublisherContext context, RecipientProviderUtilities.IDebug debug) {
        debug.send("Adding upstream committer from job %s with build number %s", run.getParent().getDisplayName(), run.getNumber());

        if (run instanceof RunWithSCM) {
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = ((RunWithSCM<?, ?>) run).getChangeSets();

            for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
                for (ChangeLogSet.Entry change : changeSet) {
                    addUserFromChangeSet(change, to, cc, bcc, env, context, debug);
                }
            }
        }
    }

    /**
     * Adds a user to the recipients list based on a specific SCM change set
     * @param change The ChangeLogSet.Entry to get the user information from
     * @param to The list of to addresses to add to
     * @param cc The list of cc addresses to add to
     * @param bcc The list of bcc addresses to add to
     * @param env The build environment
     */
    private void addUserFromChangeSet(ChangeLogSet.Entry change, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc, EnvVars env, final ExtendedEmailPublisherContext context, RecipientProviderUtilities.IDebug debug) {
        User user = change.getAuthor();
        RecipientProviderUtilities.addUsers(Collections.singleton(user), context, env, to, cc, bcc, debug);
    }

    @Extension
    @Symbol("upstreamDevelopers")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.UpstreamComitterRecipientProvider_DisplayName();
        }
    }
}
