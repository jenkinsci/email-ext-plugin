package hudson.plugins.emailext.plugins.recipients;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.Messages;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import jenkins.model.Jenkins;
import jenkins.scm.api.metadata.ContributorMetadataAction;

public class ContributorMetadataRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public ContributorMetadataRecipientProvider() {

    }

    @Override
    public void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to,
            Set<InternetAddress> cc, Set<InternetAddress> bcc) {

        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor
                    = Jenkins.get().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }

        final Debug debug = new Debug();

        ContributorMetadataAction action = context.getRun().getAction(ContributorMetadataAction.class);
        if(action != null) {
            User user = findUser(debug, action.getContributor(), action.getContributorEmail());
            if(user != null) {
                RecipientProviderUtilities.addUsers(Collections.singleton(user), context, env, to, cc, bcc, debug);
            }
        } else {
            debug.send("No ContributorMetadataAction is available");
            context.getListener().getLogger().print(Messages.ContributorMetadataRecipientProvider_NoContributorInformationAvailable());
        }    
    }

    public User findUser(RecipientProviderUtilities.IDebug debug, String author, String authorEmail) {
        // first we look for a user based on the user id (author), then if
        // that fails, we look for one based on the email.
        User user = null;
        if(!StringUtils.isBlank(author)) {
            debug.send("Trying username to get user account from Jenkins");
            user = User.get(author, false, Collections.emptyMap());
        }

        if(user == null) {
            if(!StringUtils.isBlank(authorEmail)) {
                debug.send("Trying email address to get user account from Jenkins");
                user = User.get(authorEmail, false, Collections.emptyMap());
                if (user == null) {
                    debug.send("Looking through all users for a matching email address");
                    for (User existingUser : User.getAll()) {
                        if (authorEmail.equalsIgnoreCase(getMail(existingUser))) {
                            user = existingUser;
                            break;
                        }
                    }
                }
            }
        }

        if(user == null) {
            debug.send("Could not find user with information provided");
        }

        return user;
    }

    private String getMail(User user) {
        hudson.tasks.Mailer.UserProperty property = user.getProperty(hudson.tasks.Mailer.UserProperty.class);
        if (property == null) {
            return null;
        }
        if (!property.hasExplicitlyConfiguredAddress()) {
            return null;
        }
        return property.getExplicitlyConfiguredAddress();
    }

    @Extension
    @Symbol("contributor")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.ContributorMetadataRecipientProvider_DisplayName();
        }
    }
}
