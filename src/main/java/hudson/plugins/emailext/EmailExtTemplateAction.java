package hudson.plugins.emailext;

import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Plugin;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.plugins.emailext.plugins.content.AbstractEvalContent;
import hudson.plugins.emailext.plugins.content.JellyScriptContent;
import hudson.plugins.emailext.plugins.content.ScriptContent;
import hudson.util.FormValidation;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 *
 * @author acearl
 */
public class EmailExtTemplateAction implements Action {
    
    private final AbstractProject<?,?> project;

    public EmailExtTemplateAction(AbstractProject<?,?> project) {
        this.project = project;        
    }
    
    public String getIconFileName() {
        // returning null allows us to have our own action.jelly
        return null;
    }

    public String getDisplayName() {
        return Messages.EmailExtTemplateAction_DisplayName();
    }

    public String getUrlName() {
        return "templateTest";
    }   
    
    private String renderError(Exception ex) {
        return "<h3>An error occurred trying to render the template:</h3><br/>"
                + "<span style=\"color:red; font-weight:bold\">"
                + ex.toString().replace("\n", "<br/>")
                + "</span>";
    }
    
    public FormValidation doTemplateFileCheck(@QueryParameter final String value) {
        if(!StringUtils.isEmpty(value)) {
            if(value.startsWith("managed:")) {
                return checkForManagedFile(value);
            } else {
                // first check in the default resources area...
                InputStream inputStream = Thread.currentThread().getContextClassLoader()
                		.getResourceAsStream("hudson/plugins/emailext/templates/" + value);                
                if(inputStream == null) {                
                    final File scriptsFolder = new File(Jenkins.get().getRootDir(), "email-templates");
                    final File scriptFile = new File(scriptsFolder, value);
                    try {
                        if(!scriptFile.exists() || !AbstractEvalContent.isChildOf(new FilePath(scriptFile), new FilePath(scriptsFolder))) {
                            return FormValidation.error("The file '" + value + "' does not exist");
                        }
                    } catch (IOException | InterruptedException e) {
                        //Don't want to expose too much info to a potential file fishing attempt
                        return FormValidation.error("I/O Error.");
                    }
                }
            }
        }
        return FormValidation.ok();
    }
    
    private FormValidation checkForManagedFile(final String value) {
        Plugin plugin = Jenkins.get().getPlugin("config-file-provider");
        if(plugin != null) {
            Config config = null;
            Collection<ConfigProvider> providers = getTemplateConfigProviders();
            for(ConfigProvider provider : providers) {
                for(Config c : provider.getAllConfigs()) {
                    if(c.name.equalsIgnoreCase(value)) {
                        return FormValidation.ok();
                    }                    
                }            
            }
        } else {
            return FormValidation.error(Messages.EmailExtTemplateAction_ConfigFileProviderNotAvailable());
        }
        return FormValidation.error(Messages.EmailExtTemplateAction_ManagedTemplateNotFound());
    }
    
    private static Collection<ConfigProvider> getTemplateConfigProviders() {
        Collection<ConfigProvider> providers = new ArrayList<>();
        ExtensionList<ConfigProvider> all = ConfigProvider.all();
        ConfigProvider p = all.get(GroovyTemplateConfig.GroovyTemplateConfigProvider.class);
        if(p != null) {
            providers.add(p);
        }
        
        p = all.get(JellyTemplateConfig.JellyTemplateConfigProvider.class);
        if(p != null) {
            providers.add(p);
        }
        return providers;
    }
    
    @JavaScriptMethod
    public String[] renderTemplate(String templateFile, String buildId) {
        String[] result = new String[2];
        result[0] = StringUtils.EMPTY;
        result[1] = StringUtils.EMPTY;
        
        try {
            AbstractBuild<?,?> build = project.getBuild(buildId);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            StreamTaskListener listener = new StreamTaskListener(stream);
            
            if(templateFile.endsWith(".jelly")) {
                JellyScriptContent jellyContent = new JellyScriptContent();
                jellyContent.template = templateFile;                
                result[0] = jellyContent.evaluate(build, listener, "JELLY_SCRIPT");
            } else {
                ScriptContent scriptContent = new ScriptContent();
                scriptContent.template = templateFile;                
                result[0] = scriptContent.evaluate(build, listener, "SCRIPT");                
            }
            result[1] = stream.toString(ExtendedEmailPublisher.descriptor().getCharset());
        } catch (Exception ex) {
            result[0] = renderError(ex);
        }         
        return result;
    }
    
    public AbstractProject<?, ?> getProject() {
        return project;
    }
}
