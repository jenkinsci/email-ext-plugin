package hudson.plugins.emailext.plugins.content;

/**
 * @deprecated Keeping this email content around for backwards compatibility afters the Jenkins/Hudson split.
 */
public class HudsonURLContent extends JenkinsURLContent {

    private static final String TOKEN = "HUDSON_URL";

    public String getToken() {
        return TOKEN;
    }

    public String getHelpText() {
        return "<i><b>deprecated, please use $JENKINS_URL</b></i>";
    }
}
