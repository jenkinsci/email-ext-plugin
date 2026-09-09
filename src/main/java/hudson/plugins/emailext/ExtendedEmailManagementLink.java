package hudson.plugins.emailext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.BulkChange;
import hudson.Extension;
import hudson.Functions;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.ManagementLink;
import hudson.util.FormApply;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Exposes email-ext global configuration as a standalone page in Manage Jenkins,
 * following the same pattern as the Credentials plugin's {@code GlobalCredentialsConfiguration}.
 */
@Extension(ordinal = Integer.MAX_VALUE - 300)
public class ExtendedEmailManagementLink extends ManagementLink implements Describable<ExtendedEmailManagementLink> {

    private static final Logger LOGGER = Logger.getLogger(ExtendedEmailManagementLink.class.getName());

    /**
     * Selects descriptors whose {@link GlobalConfigurationCategory} is an instance of
     * {@link Category}, so they appear on this page rather than Configure System.
     */
    @SuppressWarnings("rawtypes")
    public static final Predicate<Descriptor> FILTER = d -> d.getCategory() instanceof Category;

    @Override
    public String getIconFileName() {
        return "symbol-mail-unread-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return getDescriptor().getDisplayName();
    }

    @Override
    public String getDescription() {
        return Messages.ExtendedEmailManagementLink_Description();
    }

    @Override
    public String getUrlName() {
        return "email-ext";
    }

    @Override
    public String getCategoryName() {
        return "CONFIGURATION";
    }

    /**
     * Handles the form POST from the configuration page.
     */
    @RequirePOST
    @NonNull
    @Restricted(NoExternalUse.class)
    @SuppressWarnings("unused") // stapler web method binding
    public synchronized HttpResponse doConfigure(@NonNull StaplerRequest2 req)
            throws IOException, ServletException, FormException {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.MANAGE);
        BulkChange bc = new BulkChange(jenkins);
        try {
            boolean result = configure(req, req.getSubmittedForm());
            LOGGER.log(Level.FINE, "email-ext configuration saved: " + result);
            jenkins.save();
            return FormApply.success(
                    result ? req.getContextPath() + "/manage" : req.getContextPath() + "/" + getUrlName());
        } finally {
            bc.commit();
        }
    }

    private boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        Jenkins.get().checkPermission(Jenkins.MANAGE);
        boolean result = true;
        for (Descriptor<?> d : Functions.getSortedDescriptorsForGlobalConfigByDescriptor(FILTER)) {
            result &= configureDescriptor(req, json, d);
        }
        return result;
    }

    private boolean configureDescriptor(StaplerRequest2 req, JSONObject json, Descriptor<?> d) throws FormException {
        String name = d.getJsonSafeClassName();
        JSONObject js = json.has(name) ? json.getJSONObject(name) : new JSONObject();
        json.putAll(js);
        return d.configure(req, js);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Descriptor<ExtendedEmailManagementLink> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<ExtendedEmailManagementLink> {
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.ExtendedEmailPublisherDescriptor_DisplayName();
        }
    }

    /**
     * Custom {@link GlobalConfigurationCategory} that causes
     * {@link ExtendedEmailGlobalConfiguration} to appear on this management link's page
     * instead of the main Configure System page.
     */
    @Extension
    public static class Category extends GlobalConfigurationCategory {
        @Override
        public String getShortDescription() {
            return Messages.ExtendedEmailManagementLink_Description();
        }

        @Override
        public String getDisplayName() {
            return Messages.ExtendedEmailPublisherDescriptor_DisplayName();
        }
    }
}
