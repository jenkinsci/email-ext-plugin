package hudson.plugins.emailext.plugins.recipients;

import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.tasks.Mailer;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.logging.Logger;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by acearl on 12/25/13.
 */

public class RequesterRecipientProvider extends RecipientProvider {
    private static final Logger LOGGER = Logger.getLogger(RequesterRecipientProvider.class.getName());

    @DataBoundConstructor
    public RequesterRecipientProvider() {
        
    }
    
    @Override
    public void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        // looking for Upstream build.
        AbstractBuild<?, ?> cur = context.getBuild();
        Cause.UpstreamCause upc = context.getBuild().getCause(Cause.UpstreamCause.class);
        while (upc != null) {
            // UpstreamCause.getUpStreamProject() returns the full name, so use getItemByFullName
            AbstractProject<?, ?> p = (AbstractProject<?, ?>) Jenkins.getInstance().getItemByFullName(upc.getUpstreamProject());
            if (p == null) {
                break;
            }
            cur = p.getBuildByNumber(upc.getUpstreamBuild());
            upc = cur.getCause(Cause.UpstreamCause.class);
        }
        addUserTriggeringTheBuild(cur, to, cc, bcc, env, context.getListener());
    }

    private void addUserTriggeringTheBuild(AbstractBuild<?, ?> build, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc,
            EnvVars env, TaskListener listener) {
        User user = getByUserIdCause(build);
        if (user == null) {
            user = getByLegacyUserCause(build);
        }

        if (user != null) {
            String adrs = user.getProperty(Mailer.UserProperty.class).getAddress();
            if (adrs != null) {
                EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, adrs, env, listener);
            } else {
                listener.getLogger().println("The user does not have a configured email address, trying the user's id");
                EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, user.getId(), env, listener);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private User getByUserIdCause(AbstractBuild<?, ?> build) {
        try {
            Cause.UserIdCause cause = build.getCause(Cause.UserIdCause.class);
            if (cause != null) {
                String id = cause.getUserId();
                return User.get(id, false, null);
            }

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("deprecated")
    private User getByLegacyUserCause(AbstractBuild<?, ?> build) {
        try {
            Cause.UserCause userCause = build.getCause(Cause.UserCause.class);
            // userCause.getUserName() returns displayName which may be different from authentication name
            // Therefore use reflection to access the real authenticationName
            if (userCause != null) {
                Field authenticationName = Cause.UserCause.class.getDeclaredField("authenticationName");
                authenticationName.setAccessible(true);
                String name = (String) authenticationName.get(userCause);
                return User.get(name, false, null);
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        return null;
    }
    
    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "Requestor";
        }
        
    }
}
