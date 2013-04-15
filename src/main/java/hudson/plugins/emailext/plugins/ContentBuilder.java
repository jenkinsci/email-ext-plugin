package hudson.plugins.emailext.plugins;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.EmailToken;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.tasks.Publisher;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import org.jenkinsci.plugins.tokenmacro.TokenMacro;

/**
 * {@link Publisher} that sends notification e-mail.
 *
 * @author kyle.sweeney@valtech.com
 *
 */
public class ContentBuilder {

    private static final String DEFAULT_BODY = "\\$DEFAULT_CONTENT|\\$\\{DEFAULT_CONTENT\\}";
    private static final String DEFAULT_SUBJECT = "\\$DEFAULT_SUBJECT|\\$\\{DEFAULT_SUBJECT\\}";
    private static final String DEFAULT_RECIPIENTS = "\\$DEFAULT_RECIPIENTS|\\$\\{DEFAULT_RECIPIENTS\\}";
    private static final String DEFAULT_REPLYTO = "\\$DEFAULT_REPLYTO|\\$\\{DEFAULT_REPLYTO\\}";
    private static final String PROJECT_DEFAULT_BODY = "\\$PROJECT_DEFAULT_CONTENT|\\$\\{PROJECT_DEFAULT_CONTENT\\}";
    private static final String PROJECT_DEFAULT_SUBJECT = "\\$PROJECT_DEFAULT_SUBJECT|\\$\\{PROJECT_DEFAULT_SUBJECT\\}";
    private static final String PROJECT_DEFAULT_REPLYTO = "\\$PROJECT_DEFAULT_REPLYTO|\\$\\{PROJECT_DEFAULT_REPLYTO\\}";

    private String noNull(String string) {
        return string == null ? "" : string;
    }

    public String transformText(String origText, ExtendedEmailPublisher publisher, EmailType type, AbstractBuild<?, ?> build, TaskListener listener) {
        String defaultContent = Matcher.quoteReplacement(noNull(publisher.defaultContent));
        String defaultSubject = Matcher.quoteReplacement(noNull(publisher.defaultSubject));
        String defaultReplyTo = Matcher.quoteReplacement(noNull(publisher.replyTo));
        String defaultBody = Matcher.quoteReplacement(noNull(ExtendedEmailPublisher.DESCRIPTOR.getDefaultBody()));
        String defaultExtSubject = Matcher.quoteReplacement(noNull(ExtendedEmailPublisher.DESCRIPTOR.getDefaultSubject()));
        String defaultRecipients = Matcher.quoteReplacement(noNull(ExtendedEmailPublisher.DESCRIPTOR.getDefaultRecipients()));
        String defaultExtReplyTo = Matcher.quoteReplacement(noNull(ExtendedEmailPublisher.DESCRIPTOR.getDefaultReplyTo()));
        String newText = origText.replaceAll(
                PROJECT_DEFAULT_BODY, defaultContent).replaceAll(
                PROJECT_DEFAULT_SUBJECT, defaultSubject).replaceAll(
                PROJECT_DEFAULT_REPLYTO, defaultReplyTo).replaceAll(
                DEFAULT_BODY, defaultBody).replaceAll(
                DEFAULT_SUBJECT, defaultExtSubject).replaceAll(
                DEFAULT_RECIPIENTS, defaultRecipients).replaceAll(
                DEFAULT_REPLYTO, defaultExtReplyTo);
        
        try {
            newText = TokenMacro.expandAll(build, listener, newText, false, getPrivateMacros());
        } catch (MacroEvaluationException e) {
            listener.getLogger().println("Error evaluating token: " + e.getMessage());
        } catch (Exception e) {
            Logger.getLogger(ContentBuilder.class.getName()).log(Level.SEVERE, null, e);
        }

        return newText;
    }
    
    public static List<TokenMacro> getPrivateMacros() {
        List<TokenMacro> macros = new ArrayList<TokenMacro>();
        for (final IndexItem<EmailToken, TokenMacro> item : Index.load(EmailToken.class, TokenMacro.class)) {
            try {
                macros.add(item.instance());
            } catch (Exception e) {
                // ignore errors loading tokens
            }
        }
        return macros;
    }
}
