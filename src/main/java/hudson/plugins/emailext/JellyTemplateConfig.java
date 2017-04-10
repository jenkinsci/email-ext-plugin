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
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.JellyLanguage;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class JellyTemplateConfig extends Config {
    @Override
    public ConfigProvider getDescriptor() {
        return Jenkins.getActiveInstance().getDescriptorByType(JellyTemplateConfigProvider.class);
    }
    @DataBoundConstructor
    public JellyTemplateConfig(String id, String name, String comment, String content) {
        super(id, name, comment, content);
        ScriptApproval.get().configuring(content, JellyLanguage.get(), ApprovalContext.create().withCurrentUser());
    }

    public Object readResolve() {
        ScriptApproval.get().configuring(content, JellyLanguage.get(), ApprovalContext.create());
        return this;
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

        @NonNull
        @Override
        public Config newConfig(@Nonnull String id) {
            return new JellyTemplateConfig(id, "Jelly Email Template", "", "");
        }

        @Override
        protected String getXmlFileName() {
            return "email-ext-jelly-config-files.xml";
        }
    }
}
