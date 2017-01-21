package hudson.plugins.emailext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class GroovyTemplateConfig extends Config {
    @Override
    public ConfigProvider getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorByType(GroovyTemplateConfigProvider.class);
    }

    @DataBoundConstructor
    public GroovyTemplateConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }
    
    @Extension(optional=true)
    public static final class GroovyTemplateConfigProvider extends AbstractConfigProviderImpl {

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.GROOVY;
        }

        @Override
        public String getDisplayName() {
            return Messages.GroovyTemplateConfigProvider_DisplayName();
        }

        @NonNull
        @Override
        public Config newConfig(@Nonnull String id) {
            return new GroovyTemplateConfig(id, "Groovy Email Template", "", "");
        }
    }
}
