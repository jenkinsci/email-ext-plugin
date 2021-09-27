package hudson.plugins.emailext.plugins.recipients;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
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
 * Created by acearl on 12/25/13.
 */
public class DevelopersRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public DevelopersRecipientProvider() {

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
        Set<User> users = RecipientProviderUtilities.getChangeSetAuthors(Collections.singleton(context.getRun()), debug);
        RecipientProviderUtilities.addUsers(users, context, env, to, cc, bcc, debug);
    }

    @Extension
    @Symbol("developers")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.DevelopersRecipientProvider_DisplayName();
        }
    }
}
