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

import org.apache.commons.lang.StringUtils;

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
    private static final String ONLY_REGRESSIONS_NAME = "onlyRegressions";
    private static final boolean ONLY_REGRESSIONS_DEFAULT = false;
    public static final String MAX_LENGTH_ARG_NAME = "maxLength";

    public String getToken() {
        return TOKEN;
    }

    public List<String> getArguments() {
        return Arrays.asList(SHOW_STACK_NAME, MAX_TESTS_ARG_NAME, ONLY_REGRESSIONS_NAME);
    }

    public String getHelpText() {
        return "Displays failing unit test information, if any tests have failed.\n"
                + "<ul>\n"
                + "<li><i>" + SHOW_STACK_NAME + "</i> - indicates that "
                + "most recent builds should be at the top.<br>\n"
                + "Defaults to " + SHOW_STACK_DEFAULT + ".\n"
                + "<li><i>" + ONLY_REGRESSIONS_NAME + "</i> - indicates that "
                + "only regressions compared to the previous builds should be shown.<br>\n"
                + "Defaults to " + SHOW_STACK_DEFAULT + ".\n"
                + "<li><i>" + MAX_TESTS_ARG_NAME + "</i> - display at most this many failing tests.<br>\n"
                + "No limit is set by default.</li>\n"
                + "<li><i>" + MAX_LENGTH_ARG_NAME + "</i> - display at most this much KB of failing test data.<br/>\n"
                + "No limit is set by default. Setting \"50\" for the argument value would mean 50KB of data would be the max</li>\n"
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
            int maxLength = Args.get(args, MAX_LENGTH_ARG_NAME, Integer.MAX_VALUE);
            boolean showOldFailures = !Args.get(args, ONLY_REGRESSIONS_NAME, ONLY_REGRESSIONS_DEFAULT);
            if(maxLength < Integer.MAX_VALUE) {
                maxLength *= 1024;
            }

            if (maxTests > 0) {
                int printedTests = 0;
                int printedLength = 0;
                for (CaseResult failedTest : testResult.getFailedTests()) {
                    if (showOldFailures || failedTest.getAge() == 1) {
                        if (printedTests < maxTests && printedLength <= maxLength) {
                            printedLength += outputTest(buffer, failedTest, showStacks, maxLength-printedLength);
                            printedTests++;
                        }
                    }
                }
                if (failCount > printedTests) {
                    buffer.append("... and ");
                    buffer.append(failCount - printedTests);
                    buffer.append(" other failed tests.\n\n");
                }
                if (printedLength >= maxLength) {
                    buffer.append("\n\n... output truncated.\n\n");
                }
            }
        }

        return buffer.toString();
    }

    private int outputTest(StringBuffer buffer, CaseResult failedTest,
            boolean showStack, int lengthLeft) {
        StringBuffer local = new StringBuffer();
        int currLength = buffer.length();

        local.append(failedTest.getStatus().toString());
        local.append(":  ");
        
        local.append(failedTest.getClassName());
        local.append(".");

        local.append(failedTest.getDisplayName());
        local.append("\n\n");

        local.append("Error Message:\n");
        local.append(failedTest.getErrorDetails());

        if (showStack) {
            local.append("\n\n");
            local.append("Stack Trace:\n");
            local.append(failedTest.getErrorStackTrace());
        }

        local.append("\n\n");

        if(local.length() > lengthLeft) {
            local.setLength(lengthLeft);
        }

        buffer.append(local.toString());
        return local.length();
    }

    public boolean hasNestedContent() {
        return false;
    }
}
