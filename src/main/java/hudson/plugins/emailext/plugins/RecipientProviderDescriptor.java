package hudson.plugins.emailext.plugins;

import hudson.model.Descriptor;
import hudson.model.Job;

/**
 * @author acearl
 */
public abstract class RecipientProviderDescriptor extends Descriptor<RecipientProvider> {

    protected RecipientProviderDescriptor() {
    }

    public boolean isApplicable(Class<? extends Job> jobType) {
        return true;
    }
}
