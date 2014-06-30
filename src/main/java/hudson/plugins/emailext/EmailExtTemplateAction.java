package hudson.plugins.emailext;

import hudson.ExtensionList;
import hudson.Plugin;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.content.JellyScriptContent;
import hudson.plugins.emailext.plugins.content.ScriptContent;
import hudson.util.FormValidation;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
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
        return "/plugin/email-ext/images/template-debugger.png";
    }

    public String getDisplayName() {
        return Messages.EmailExtTemplateAction_DisplayName();
    }

    public String getUrlName() {
        return "templateTest";
    }   
    
    private String renderError(Exception ex) {
        StringBuilder builder = new StringBuilder();
        builder.append("<h3>An error occured trying to render the template:</h3><br/>");
        builder.append("<span style=\"color:red; font-weight:bold\">");
        builder.append(ex.toString().replace("\n", "<br/>"));
        builder.append("</span>");
        return builder.toString();
    }
    
    public FormValidation doTemplateFileCheck(@QueryParameter final String value) {
        if(!StringUtils.isEmpty(value)) {
            if(value.startsWith("managed:")) {
                return checkForManagedFile(value);
            } else {
                final File scriptsFolder = new File(Hudson.getInstance().getRootDir(), "email-templates");
                final File scriptFile = new File(scriptsFolder, value);
                if(!scriptFile.exists()) {
                    return FormValidation.error("The file '" + value + "' does not exist");
                }
            }
        }
        return FormValidation.ok();
    }
    
    private FormValidation checkForManagedFile(final String value) {
        Plugin plugin = Jenkins.getInstance().getPlugin("config-file-provider");
        if(plugin != null) {
            Config config = null;
            Collection<ConfigProvider> providers = getTemplateConfigProviders();
            for(ConfigProvider provider : providers) {
                for(Config c : provider.getAllConfigs()) {
                    if(c.name.equalsIgnoreCase(value) && provider.isResponsibleFor(c.id)) {
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
        Collection<ConfigProvider> providers = new ArrayList<ConfigProvider>(1);
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
    public String renderTemplate(String templateFile, String buildId) {
        String result;
        
        try {
            AbstractBuild<?,?> build = project.getBuild(buildId);
            if(templateFile.endsWith(".jelly")) {
                JellyScriptContent jellyContent = new JellyScriptContent();
                jellyContent.template = templateFile;
                result = jellyContent.evaluate(build, TaskListener.NULL, "JELLY_SCRIPT");
            } else {
                ScriptContent scriptContent = new ScriptContent();
                scriptContent.template = templateFile;                
                result = scriptContent.evaluate(build, TaskListener.NULL, "SCRIPT");
            }
        } catch (Exception ex) {
            result = renderError(ex);
        } 
        
        return result;
    }
    
    public AbstractProject<?, ?> getProject() {
        return project;
    }
}
