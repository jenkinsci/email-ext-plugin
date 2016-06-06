package hudson.plugins.emailext.plugins;

import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.model.Job;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        return Jenkins.getActiveInstance().getDescriptorList(RecipientProvider.class);
    }

    public static List<RecipientProviderDescriptor> allSupporting(Class<? extends Job> clazz) {
        List<RecipientProviderDescriptor> rt = new ArrayList<>();
        for (RecipientProviderDescriptor recipientProviderDescriptor : all()) {
            if (recipientProviderDescriptor.isApplicable(clazz)) {
                rt.add(recipientProviderDescriptor);
            }
        }
        return rt;
    }

    @SuppressWarnings("unchecked")
    public static List<RecipientProviderDescriptor> allSupporting(String clazz) {
        try {
            return allSupporting((Class<? extends Job>) Class.forName(clazz));
        } catch (ClassNotFoundException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public RecipientProviderDescriptor getDescriptor() {
        return (RecipientProviderDescriptor) super.getDescriptor();
    }

    public abstract void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc);
}
