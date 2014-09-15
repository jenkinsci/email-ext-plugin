package hudson.plugins.emailext.plugins.content;

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import java.io.IOException;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@EmailToken
public class JenkinsURLContent extends DataBoundTokenMacro {

    private static final String MACRO_NAME = "JENKINS_URL";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME) || macroName.equals("HUDSON_URL");
    }

    public String getHelpText() {
        return "Displays the URL to the Jenkins server. (You can change this on the system configuration page.)";
    }

    @Override
    public String evaluate(Run<?, ?> context, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        // JENKINS-6193 - Only override the global URL if we should override global settings
        String jenkinsUrl = Jenkins.getInstance().getRootUrl();
        
        ExtendedEmailPublisher publisher = context.getParent().getPublishersList().get(ExtendedEmailPublisher.class);
        
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
