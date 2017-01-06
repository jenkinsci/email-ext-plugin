/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext.plugins.recipients;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Job;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author acearl
 */
public class ListRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public ListRecipientProvider() {
    }

    private String recipientList;
    
    @DataBoundSetter
    public void setRecipientList(String recipientList){
         this.recipientList = recipientList;
    }
    
    @Override
    public void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        try {
            ExtendedEmailPublisherDescriptor descriptor = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
            descriptor.debug(context.getListener().getLogger(), "Adding recipients from project recipient list");
            String recipients = recipientList;
            if(recipients == null ){
                recipientList = EmailRecipientUtils.getRecipientList(context, context.getPublisher().recipientList);
            }
            EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc,recipients , env, context.getListener());
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
        /*
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return !jobType.getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowJob");
        }
        */
    }
}
