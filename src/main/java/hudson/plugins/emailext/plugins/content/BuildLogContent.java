package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.tasks.Mailer;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An EmailContent for build log. Shows last 250 lines of the build log file.
 * 
 * @author dvrzalik
 */
public class BuildLogContent implements EmailContent {
    
    private static final int MAX_LINES = 250;
    
    private static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

    public String getToken() {
        return "BUILD_LOG";
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(AbstractBuild<P, B> build, EmailType emailType) {
        
        StringBuffer buffer = new StringBuffer();
        try {
            List<String> lines = build.getLog(MAX_LINES);
            for(String line: lines) {
                //TODO: show file links the same way as MailSender
                buffer.append(line);
                buffer.append('\n');
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        return buffer.toString();
    }

    public boolean hasNestedContent() {
        return false;
    }

    public String getHelpText() {
        return "Displays last " + MAX_LINES + " lines of the build log";
    }
}
