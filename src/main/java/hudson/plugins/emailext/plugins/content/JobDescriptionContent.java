package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JobDescriptionContent implements EmailContent {

    private static final String TOKEN = "JOB_DESCRIPTION";

    public String getToken() {
        return TOKEN;
    }

    public List<String> getArguments() {
        return Collections.emptyList();
    }

    public String getHelpText() {
        return "Displays the description of the job.";
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
            EmailType emailType, Map<String, ?> args) {
        return build.getParent().getDescription();
    }

    public boolean hasNestedContent() {
        return false;
    }
}
