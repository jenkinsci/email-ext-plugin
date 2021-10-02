package hudson.plugins.emailext.plugins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Job;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

/**
 * Created by acearl on 12/24/13.
 */
public abstract class RecipientProvider extends AbstractDescribableImpl<RecipientProvider> implements ExtensionPoint {
    private static final Logger LOG = Logger.getLogger(RecipientProvider.class.getName());
    
    public static DescriptorExtensionList<RecipientProvider, RecipientProviderDescriptor> all() {
        return Jenkins.get().getDescriptorList(RecipientProvider.class);
    }

    public static List<RecipientProviderDescriptor> allSupporting(Class<? extends Job<?, ?>> clazz) {
        List<RecipientProviderDescriptor> rt = new ArrayList<>();
        for (RecipientProviderDescriptor recipientProviderDescriptor : all()) {
            try {
                if (recipientProviderDescriptor.isApplicable(clazz)) {
                    rt.add(recipientProviderDescriptor);
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, MessageFormat.format("Exception checking if {0} supports {1}, skipping",
                        recipientProviderDescriptor.getDisplayName(), clazz.getName()), ex);
            }
        }
        return rt;
    }

    public static void checkAllSupport(@NonNull List<? extends RecipientProvider> providers, Class<? extends Job> clazz) {
        Set<String> notSupported = new TreeSet<>();
        for (RecipientProvider provider : providers) {
            if (!provider.getDescriptor().isApplicable(clazz)) {
                notSupported.add(provider.getClass().getName());
            }
        }

        if (!notSupported.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.
                    format("The following recipient providers do not support {0} {1}", clazz.getName(),
                            StringUtils.join(notSupported, ", ")));
        }
    }

    @Override
    public RecipientProviderDescriptor getDescriptor() {
        return (RecipientProviderDescriptor) super.getDescriptor();
    }

    public abstract void addRecipients(ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc);
}
