package hudson.plugins.emailext.plugins;

import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;

import java.util.Set;
import javax.mail.internet.InternetAddress;

import hudson.DescriptorExtensionList;
import hudson.model.AbstractDescribableImpl;
import jenkins.model.Jenkins;

/**
 * Created by acearl on 12/24/13.
 */
public abstract class RecipientProvider extends AbstractDescribableImpl<RecipientProvider> implements ExtensionPoint {
    
    public static DescriptorExtensionList<RecipientProvider, RecipientProviderDescriptor> all() {
        return Jenkins.getActiveInstance().<RecipientProvider, RecipientProviderDescriptor>getDescriptorList(RecipientProvider.class);
    }

    @Override
    public RecipientProviderDescriptor getDescriptor() {
        return (RecipientProviderDescriptor) super.getDescriptor();
    }

    public abstract void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc);
}
