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

        ContributorMetadataAction contributor = context.getRun().getAction(ContributorMetadataAction.class);
        if(contributor != null) {
            User user = User.getById(contributor.getContributor(), false);
            if(user == null) {
                // just add the email address if set
                if(!StringUtils.isBlank(contributor.getContributorEmail())) {
                    try {
                        to.add(new InternetAddress(contributor.getContributorEmail()));
                    } catch(AddressException e) {
                        context.getListener().error(Messages.ErrorAddingContributorAddress(e.getMessage()));
                        debug.send(Messages.ErrorAddingContributorAddress(e.toString()));
                    }
                }
            } else {
                RecipientProviderUtilities.addUsers(Collections.singleton(user), context, env, to, cc, bcc, debug);
            }
        } else {
            context.getListener().getLogger().print(Messages.NoContributorInformationAvailable());
        }    
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
