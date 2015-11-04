package hudson.plugins.emailext.plugins.recipients;

import hudson.model.AbstractBuild;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.scm.ChangeLogSet;
import java.util.HashSet;
import java.util.Set;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by acearl on 12/25/13.
 */
public class DevelopersRecipientProvider extends RecipientProvider {
    
    @DataBoundConstructor
    public DevelopersRecipientProvider() {
        
    }
    
    @Override
    public void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        ExtendedEmailPublisherDescriptor descriptor = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        Set<User> users = new HashSet<User>();

        if (context.getRun() instanceof AbstractBuild) {
            AbstractBuild<?, ?> build = (AbstractBuild) context.getRun();
            for (ChangeLogSet.Entry change : build.getChangeSet()) {
                users.add(change.getAuthor());
            }
        }

        for (User user : users) {
            if (!EmailRecipientUtils.isExcludedRecipient(user, context.getListener())) {
                String userAddress = EmailRecipientUtils.getUserConfiguredEmail(user);
                if (userAddress != null) {
                    descriptor.debug(context.getListener().getLogger(), "Adding user address %s, they were not considered an excluded committer", userAddress);
                    EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, userAddress, env, context.getListener());
                } else {
                    context.getListener().getLogger().println("Failed to send e-mail to " + user.getFullName() + " because no e-mail address is known, and no default e-mail domain is configured");
                }
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        @Override
        public String getDisplayName() {
            return "Developers";
        }       
    }
}
