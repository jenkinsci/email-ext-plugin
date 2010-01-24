package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CauseContent
        implements EmailContent {
    private static final String TOKEN = "CAUSE";

    public String getToken() {
        return TOKEN;
    }

    public List<String> getArguments() {
        return Collections.emptyList();
    }

    public String getHelpText() {
        return "Displays the cause of the build.\n";
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
    String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
                      EmailType emailType, Map<String, ?> args) {
        List<Cause> causes = new LinkedList<Cause>();
        CauseAction causeAction = build.getAction(CauseAction.class);
        if (causeAction != null) {
            causes = causeAction.getCauses();
        }

        return formatCauses(causes);
    }

    private String formatCauses(List<Cause> causes) {
        if (causes.isEmpty()) {
            return "N/A";
        }

        List<String> causeNames = new LinkedList<String>();
        for (Cause cause : causes) {
            causeNames.add(cause.getShortDescription());
        }

        return StringUtils.join(causeNames, ", ");
    }

    public boolean hasNestedContent() {
        return false;
    }

}
