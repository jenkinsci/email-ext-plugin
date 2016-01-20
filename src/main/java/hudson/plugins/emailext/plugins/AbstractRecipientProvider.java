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
public abstract class AbstractRecipientProvider extends AbstractDescribableImpl<AbstractRecipientProvider> implements ExtensionPoint {
    
    public static DescriptorExtensionList<AbstractRecipientProvider, AbstractRecipientProviderDescriptor> all() {
        return Jenkins.getActiveInstance().<AbstractRecipientProvider, AbstractRecipientProviderDescriptor>getDescriptorList(AbstractRecipientProvider.class);
    }

    @Override
    public AbstractRecipientProviderDescriptor getDescriptor() {
        return (AbstractRecipientProviderDescriptor) super.getDescriptor();
    }

    public abstract void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc);
}
