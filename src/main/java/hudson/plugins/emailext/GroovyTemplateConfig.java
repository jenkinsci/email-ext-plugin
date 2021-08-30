package hudson.plugins.emailext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.kohsuke.stapler.DataBoundConstructor;

public class GroovyTemplateConfig extends Config {
    @Override
    public ConfigProvider getDescriptor() {
        return Jenkins.get().getDescriptorByType(GroovyTemplateConfigProvider.class);
    }

    @DataBoundConstructor
    public GroovyTemplateConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
        ScriptApproval.get().configuring(content, GroovyLanguage.get(), ApprovalContext.create().withCurrentUser());
    }

    public Object readResolve() {
        ScriptApproval.get().configuring(content, GroovyLanguage.get(), ApprovalContext.create());
        return this;
    }
    
    @Extension(optional=true)
    public static final class GroovyTemplateConfigProvider extends AbstractConfigProviderImpl {

        public GroovyTemplateConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return ContentType.DefinedType.GROOVY;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.GroovyTemplateConfigProvider_DisplayName();
        }

        @NonNull
        @Override
        public Config newConfig(@NonNull String id) {
            return new GroovyTemplateConfig(id, "Groovy Email Template", "", "");
        }

        @Deprecated
        @Override
        protected String getXmlFileName() {
            return "email-ext-groovy-config-files.xml";
        }
    }
}
