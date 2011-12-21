package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An EmailContent for failing tests. Only shows tests that have failed.
 * 
 * @author markltbaker
 */
public class FailedTestsContent implements EmailContent {

    private static final String TOKEN = "FAILED_TESTS";

    private static final String SHOW_STACK_NAME = "showStack";
    private static final boolean SHOW_STACK_DEFAULT = true;
    public static final String MAX_TESTS_ARG_NAME = "maxTests";

    public String getToken() {
        return TOKEN;
    }

    public List<String> getArguments() {
        return Arrays.asList(SHOW_STACK_NAME, MAX_TESTS_ARG_NAME);
    }

    public String getHelpText() {
        return "Displays failing unit test information, if any tests have failed.\n"
                + "<ul>\n"
                + "<li><i>" + SHOW_STACK_NAME + "</i> - indicates that "
                + "most recent builds should be at the top.<br>\n"
                + "Defaults to " + SHOW_STACK_DEFAULT + ".\n"
                + "<li><i>" + MAX_TESTS_ARG_NAME + "</i> - display at most this many failing tests.<br>\n"
                + "No limit is set by default.\n"
                + "</ul>\n";
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(
            AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
            EmailType emailType, Map<String, ?> args) {

        StringBuffer buffer = new StringBuffer();
        AbstractTestResultAction<?> testResult = build.getTestResultAction();

        if (null == testResult) {
            return "No tests ran.";
        }

        int failCount = testResult.getFailCount();

        if (failCount == 0) {
            buffer.append("All tests passed");
        } else {
            buffer.append(failCount);
            buffer.append(" tests failed.");
            buffer.append('\n');

            boolean showStacks = Args.get(args, SHOW_STACK_NAME, SHOW_STACK_DEFAULT);
            int maxTests = Args.get(args, MAX_TESTS_ARG_NAME, Integer.MAX_VALUE);
            if (maxTests > 0) {
                int printedTests = 0;
                for (CaseResult failedTest : testResult.getFailedTests()) {
                    if (printedTests < maxTests) {
                        outputTest(buffer, failedTest, showStacks);
                        printedTests++;
                    }
                }
                if (failCount > printedTests) {
                    buffer.append("... and ");
                    buffer.append(failCount - printedTests);
                    buffer.append(" other failed tests.\n\n");
                }
            }
        }

        return buffer.toString();
    }

    private void outputTest(StringBuffer buffer, CaseResult failedTest,
            boolean showStack) {
        buffer.append(failedTest.getStatus().toString());
        buffer.append(":  ");
        buffer.append(failedTest.getClassName());
        buffer.append(".");
        buffer.append(failedTest.getDisplayName());
        buffer.append("\n\n");
        buffer.append("Error Message:\n");
        buffer.append(failedTest.getErrorDetails());
        if (showStack) {
            buffer.append("\n\nStack Trace:\n");
            buffer.append(failedTest.getErrorStackTrace());
        }
        buffer.append("\n\n");
    }

    public boolean hasNestedContent() {
        return false;
    }
}
