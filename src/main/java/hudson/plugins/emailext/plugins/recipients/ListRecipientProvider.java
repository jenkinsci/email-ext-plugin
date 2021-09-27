/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext.plugins.recipients;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Job;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.Messages;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author acearl
 */
public class ListRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public ListRecipientProvider() {

    }

    @Override
    public void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        try {
            ExtendedEmailPublisherDescriptor descriptor = Jenkins.get().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
            descriptor.debug(context.getListener().getLogger(), "Adding recipients from project recipient list");
            EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, EmailRecipientUtils.getRecipientList(context, context.getPublisher().recipientList), env, context.getListener());
        } catch (MessagingException ex) {
            Logger.getLogger(ListRecipientProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Extension
    @Symbol("recipients")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.ListRecipientProvider_DisplayName();
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return !jobType.getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowJob");
        }
    }
}
