package hudson.plugins.emailext.plugins.content;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import java.io.IOException;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension
public class JenkinsURLContent extends DataBoundTokenMacro {

    private static final String MACRO_NAME = "JENKINS_URL";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME) || macroName.equals("HUDSON_URL");
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        // JENKINS-6193 - Only override the global URL if we should override global settings
        String jenkinsUrl = Jenkins.getActiveInstance().getRootUrl();
        
        ExtendedEmailPublisher publisher = context.getProject().getPublishersList().get(ExtendedEmailPublisher.class);
        
        if (publisher.getDescriptor().getOverrideGlobalSettings()) {
            jenkinsUrl = publisher.getDescriptor().getHudsonUrl();
        }

        if (jenkinsUrl == null) {
            return "";
        }
        if (!jenkinsUrl.endsWith("/")) {
            jenkinsUrl += "/";
        }

        return Util.encode(jenkinsUrl);
    }
}
