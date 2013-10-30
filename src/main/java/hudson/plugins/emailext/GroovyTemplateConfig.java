package hudson.plugins.emailext;

import hudson.Extension;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.DataBoundConstructor;


public class GroovyTemplateConfig extends Config {
    
    @DataBoundConstructor
    public GroovyTemplateConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
    }
    
    @Extension
    public static final class GroovyTemplateConfigProvider extends AbstractConfigProviderImpl {

        public GroovyTemplateConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.GROOVY;
        }

        @Override
        public String getDisplayName() {
            return Messages.GroovyTemplateConfigProvider_DisplayName();
        }

        @Override
        public Config newConfig() {
            String id = getProviderId() + System.currentTimeMillis();
            return new GroovyTemplateConfig(id, "Groovy Email Template", "", "");
        }

        @Override
        protected String getXmlFileName() {
            return "email-ext-groovy-config-files.xml";
        }
    }    
}
