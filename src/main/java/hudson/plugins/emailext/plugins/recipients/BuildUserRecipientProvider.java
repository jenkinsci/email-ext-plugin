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
import java.io.PrintStream;
import java.util.Collections;
import java.util.Set;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by skroell on 03/03/2020.
 */

public class BuildUserRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public BuildUserRecipientProvider() {

    }

    @Override
    public void addRecipients(final ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        
        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor = Jenkins.get().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }

        final Debug debug = new Debug();

        // Don't look upstream, find cause of current build
        // Difference between this and RequesterRecipientProvider is that we send emails to the
        // user triggering ex. a rebuild and not the original upstream user.
        Run<?, ?> cur = context.getRun();
        Cause.UserIdCause upc = cur.getCause(hudson.model.Cause.UserIdCause.class);
        if(upc == null){
            context.getListener().getLogger().print("The build was not caused by a user!");
        }
        addUserTriggeringTheBuild(cur, to, cc, bcc, env, context, debug);
    }

    private static void addUserTriggeringTheBuild(Run<?, ?> run, Set<InternetAddress> to,
        Set<InternetAddress> cc, Set<InternetAddress> bcc, EnvVars env, final ExtendedEmailPublisherContext context, RecipientProviderUtilities.IDebug debug) {

        final User user = RecipientProviderUtilities.getUserTriggeringTheBuild(run);
        if (user != null) {
            RecipientProviderUtilities.addUsers(Collections.singleton(user), context, env, to, cc, bcc, debug);
        }
    }

    @Extension
    @Symbol("buildUser")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.BuildUserRecipientProvider_DisplayName();
        }
    }
}
