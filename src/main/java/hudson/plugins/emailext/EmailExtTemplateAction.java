package hudson.plugins.emailext;

import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Plugin;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Item;
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
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 *
 * @author acearl
 */
public class EmailExtTemplateAction implements Action {

    private final AbstractProject<?, ?> project;

    public EmailExtTemplateAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    @Override
    public String getIconFileName() {
        // returning null allows us to have our own action.jelly
        return null;
    }

    @Override
    public String getDisplayName() {
        return Messages.EmailExtTemplateAction_DisplayName();
    }

    @Override
    public String getUrlName() {
        return "templateTest";
    }

    private String renderError(Exception ex) {
        String message = (ex != null) ? ex.getClass().getSimpleName() + ": " + ex.getMessage() : "Unknown error";

        String escaped = Util.xmlEscape(message).replace("\n", "<br/>");

        return "<h3>An error occurred while rendering the template:</h3><br/>"
                + "<span style=\"color:red; font-weight:bold\">"
                + escaped
                + "</span>";
    }

    @SuppressWarnings("lgtm[jenkins/csrf]")
    public FormValidation doTemplateFileCheck(@QueryParameter final String value) {
        // See src/main/resources/hudson/plugins/emailext/EmailExtTemplateAction/{index,action}.groovy
        if (Jenkins.get()
                .getDescriptorByType(ExtendedEmailPublisherDescriptor.class)
                .isAdminRequiredForTemplateTesting()) {
            Jenkins.get().checkPermission(Jenkins.MANAGE);
        } else {
            project.checkPermission(Item.CONFIGURE);
        }

        if (!StringUtils.isEmpty(value)) {
            if (value.startsWith("managed:")) {
                return checkForManagedFile(StringUtils.removeStart(value, "managed:"));
            } else {
                // first check in the default resources area...
                InputStream inputStream = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("hudson/plugins/emailext/templates/" + value);
                if (inputStream == null) {
                    final File scriptsFolder = new File(Jenkins.get().getRootDir(), "email-templates");
                    final File scriptFile = new File(scriptsFolder, value);
                    try {
                        if (!scriptFile.exists()
                                || !AbstractEvalContent.isChildOf(
                                        new FilePath(scriptFile), new FilePath(scriptsFolder))) {
                            return FormValidation.error("The file '" + value + "' does not exist");
                        }
                    } catch (IOException | InterruptedException e) {
                        // Don't want to expose too much info to a potential file fishing attempt
                        return FormValidation.error("I/O Error.");
                    }
                }
            }
        }
        return FormValidation.ok();
    }

    private FormValidation checkForManagedFile(final String value) {
        Plugin plugin = Jenkins.get().getPlugin("config-file-provider");
        if (plugin != null) {
            Config config = null;
            Collection<ConfigProvider> providers = getTemplateConfigProviders();
            for (ConfigProvider provider : providers) {
                for (Config c : provider.getAllConfigs()) {
                    if (c.name.equalsIgnoreCase(value)) {
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
        if (p != null) {
            providers.add(p);
        }

        p = all.get(JellyTemplateConfig.JellyTemplateConfigProvider.class);
        if (p != null) {
            providers.add(p);
        }
        return providers;
    }

    @RequirePOST
    @SuppressWarnings("lgtm[jenkins/csrf]")
    public void doRenderTemplate(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException {
        if (Jenkins.get()
                .getDescriptorByType(ExtendedEmailPublisherDescriptor.class)
                .isAdminRequiredForTemplateTesting()) {
            Jenkins.get().checkPermission(Jenkins.MANAGE);
        } else {
            project.checkPermission(Item.CONFIGURE);
        }

        String templateFile = req.getParameter("templateFile");
        String buildId = req.getParameter("buildId");

        JSONObject result = new JSONObject();
        result.put("renderedContent", StringUtils.EMPTY);
        result.put("consoleOutput", StringUtils.EMPTY);

        try {
            AbstractBuild<?, ?> build = project.getBuild(buildId);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            StreamTaskListener listener = new StreamTaskListener(stream);

            String renderedContent;
            if (templateFile.endsWith(".jelly")) {
                JellyScriptContent jellyContent = new JellyScriptContent();
                jellyContent.template = templateFile;
                renderedContent = jellyContent.evaluate(build, listener, "JELLY_SCRIPT");
            } else {
                ScriptContent scriptContent = new ScriptContent();
                scriptContent.template = templateFile;
                renderedContent = scriptContent.evaluate(build, listener, "SCRIPT");
            }
            result.put("renderedContent", renderedContent);
            result.put(
                    "consoleOutput",
                    hudson.Util.xmlEscape(
                            stream.toString(ExtendedEmailPublisher.descriptor().getCharset())));
        } catch (Exception ex) {
            result.put("renderedContent", renderError(ex));
        }

        rsp.setContentType("application/json");
        rsp.setCharacterEncoding("UTF-8");
        rsp.getWriter().print(result.toString());
    }

    public AbstractProject<?, ?> getProject() {
        return project;
    }
}
