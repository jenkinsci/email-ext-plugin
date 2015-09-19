package hudson.plugins.emailext.plugins.recipients;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.Messages;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.internet.InternetAddress;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sends emails to committers of upstream builds which triggered this build.
 */
public class UpstreamComitterRecipientProvider extends RecipientProvider {
    private static final ExtendedEmailPublisherDescriptor descriptor = Jenkins.getInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

    @DataBoundConstructor
    public UpstreamComitterRecipientProvider() {
    }

    @Override
    public void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        descriptor.debug(context.getListener().getLogger(), "Sending email to upstream committer(s).");
        Run<?, ?> cur;
        Cause.UpstreamCause upc = context.getRun().getCause(Cause.UpstreamCause.class);
        while (upc != null) {
            Job<?, ?> p = (Job<?, ?>) Jenkins.getInstance().getItemByFullName(upc.getUpstreamProject());
            if(p == null) {
                context.getListener().getLogger().print("There is a break in the project linkage, could not retrieve upstream project information");
                break;
            }
            cur = p.getBuildByNumber(upc.getUpstreamBuild());
            upc = cur.getCause(Cause.UpstreamCause.class);
            addUpstreamCommittersTriggeringBuild(cur, to, cc, bcc, env, context.getListener());
        }
    }

    /**
     * Adds for the given upstream build the committers to the recipient list for each commit in the upstream build.
     *
     * @param build the upstream build
     * @param to the to recipient list
     * @param cc the cc recipient list
     * @param bcc the bcc recipient list
     * @param env
     * @param listener
     */
    private void addUpstreamCommittersTriggeringBuild(Run<?, ?> build, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc, EnvVars env, TaskListener listener) {
        descriptor.debug(listener.getLogger(), "Adding upstream committer from job %s with build number %s", build.getParent().getDisplayName(), build.getNumber());

        List<ChangeLogSet<?>> changeSets = new ArrayList<ChangeLogSet<?>>();
        if(build instanceof AbstractBuild<?,?>) {
            AbstractBuild<?,?> b = (AbstractBuild<?,?>)build;
            changeSets.add(b.getChangeSet());
        } else {
            try {
                // check for getChangeSets which WorkflowRun has
                Method m = build.getClass().getMethod("getChangeSets");
                changeSets = (List<ChangeLogSet<? extends ChangeLogSet.Entry>>)m.invoke(build);
            } catch (NoSuchMethodException e) {
                listener.getLogger().print("Could not add upstream committers, build type does not provide change set");
            } catch (InvocationTargetException e) {
                listener.getLogger().print("Could not add upstream committers, build type does not provide change set");
            } catch (IllegalAccessException e) {
                listener.getLogger().print("Could not add upstream committers, build type does not provide change set");
            }
        }

        if(!changeSets.isEmpty()) {
            for(ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
                for(ChangeLogSet.Entry change : changeSet) {
                    addUserFromChangeSet(change, to, cc, bcc, env, listener);
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
     * @param listener The listener for logging
     */
    private void addUserFromChangeSet(ChangeLogSet.Entry change, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc, EnvVars env, TaskListener listener) {
        User user = change.getAuthor();
        String email = user.getProperty(Mailer.UserProperty.class).getAddress();
        if (email != null) {
            descriptor.debug(listener.getLogger(), "Adding upstream committer %s to recipient list with email %s", user.getFullName(), email);
            EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, email, env, listener);
        } else {
            descriptor.debug(listener.getLogger(), "The user %s does not have a configured email email, trying the user's id", user.getFullName());
            EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, user.getId(), env, listener);
        }
    }

    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.UpstreamComitterRecipientProvider_DisplayName();
        }
    }
}
