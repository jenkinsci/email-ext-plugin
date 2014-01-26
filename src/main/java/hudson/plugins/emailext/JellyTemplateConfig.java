package hudson.plugins.emailext;

import hudson.Extension;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.DataBoundConstructor;

public class JellyTemplateConfig extends Config {
    
    @DataBoundConstructor
    public JellyTemplateConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }
    
    @Extension(optional=true)
    public static final class JellyTemplateConfigProvider extends AbstractConfigProviderImpl {

        public JellyTemplateConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.XML;
        }

        @Override
        public String getDisplayName() {
            return Messages.JellyTemplateConfigProvider_DisplayName();
        }

        @Override
        public Config newConfig() {
            String id = getProviderId() + System.currentTimeMillis();
            return new JellyTemplateConfig(id, "Jelly Email Template", "", "");
        }

        @Override
        protected String getXmlFileName() {
            return "email-ext-jelly-config-files.xml";
        }
    }    
}
