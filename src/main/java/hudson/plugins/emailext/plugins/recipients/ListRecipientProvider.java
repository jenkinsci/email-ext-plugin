/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext.plugins.recipients;

import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.EnvVars;
import hudson.Extension;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
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
            ExtendedEmailPublisherDescriptor descriptor = Jenkins.getInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
            descriptor.debug(context.getListener().getLogger(), "Adding recipients from project recipient list");
            EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, EmailRecipientUtils.getRecipientList(context, context.getPublisher().recipientList), env, context.getListener());
        } catch (MessagingException ex) {
            Logger.getLogger(ListRecipientProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        
        @Override
        public String getDisplayName() {
            return "Recipient List";
        }
    }
}
