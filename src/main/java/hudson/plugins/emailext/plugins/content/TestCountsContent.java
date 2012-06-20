package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.tasks.test.AbstractTestResultAction;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Displays the number of tests.
 *
 * @author Seiji Sogabe
 */
public class TestCountsContent implements EmailContent {

    private static final String TOKEN = "TEST_COUNTS";

    private static final String VAR_ARG_NAME = "var";

    private static final String VAR_DEFAULT_VALUE = "total";

    public String getToken() {
        return TOKEN;
    }

    public List<String> getArguments() {
        return Collections.singletonList(VAR_ARG_NAME);
    }

    public String getHelpText() {
        return "Displays the number of tests.\n"
                + "<ul>\n"
                + "<li><i>" + VAR_ARG_NAME + "</i> - Defaults to \"" + VAR_DEFAULT_VALUE + "\".\n"
                + "  <ul>\n"
                + "    <li>total - the number of all tests. </li>\n"
                + "    <li>pass - the number of passed tests. </li>\n"
                + "    <li>fail - the number of failed tests.</li>\n"
                + "    <li>skip - the number of skipped tests.</li> \n"
                + "  </ul>\n"
                + "</li>\n"
                + "</ul>\n";
    }

    public boolean hasNestedContent() {
        return false;
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher, EmailType emailType, Map<String, ?> args)
            throws IOException, InterruptedException {

        AbstractTestResultAction<?> action = build.getTestResultAction();
        if (action == null) {
            return "";
        }

        String arg = Args.get(args, VAR_ARG_NAME, VAR_DEFAULT_VALUE).toLowerCase();
        
        if ("total".equals(arg)) {
            return String.valueOf(action.getTotalCount());
        } else if ("pass".equals(arg)) {
            return String.valueOf(action.getTotalCount()-action.getFailCount()-action.getSkipCount());
        } else if ("fail".equals(arg)) {
            return String.valueOf(action.getFailCount());
        } else if ("skip".equals(arg)) {
            return String.valueOf(action.getSkipCount());
        }

        return "";
    }
}
