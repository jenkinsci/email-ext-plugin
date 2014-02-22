package hudson.plugins.emailext.plugins;

import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import jenkins.model.Jenkins;


import java.util.Set;
import javax.mail.internet.InternetAddress;

/**
 * Created by acearl on 12/24/13.
 */
public abstract class RecipientProvider implements Describable<RecipientProvider>, ExtensionPoint {
    
    public static DescriptorExtensionList<RecipientProvider, RecipientProviderDescriptor> all() {
        return Jenkins.getInstance().<RecipientProvider, RecipientProviderDescriptor>getDescriptorList(RecipientProvider.class);
    }
    
    public RecipientProviderDescriptor getDescriptor() {
        return (RecipientProviderDescriptor) Jenkins.getInstance().getDescriptor(getClass());
    }

    public abstract void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc);
}
