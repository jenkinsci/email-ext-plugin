package hudson.plugins.emailext.plugins.content;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;

/**
 * An EmailContent for failing tests. Only shows tests that have failed.
 * 
 * @author markltbaker
 */
@Extension
public class FailedTestsContent extends DataBoundTokenMacro {

    @Parameter
    public boolean showStack = true;

    @Parameter
    public boolean showMessage = true;

    @Parameter
    public int maxTests = Integer.MAX_VALUE;

    @Parameter
    public boolean onlyRegressions = false;

    @Parameter
    public int maxLength = Integer.MAX_VALUE;

    public static final String MACRO_NAME = "FAILED_TESTS";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        StringBuilder buffer = new StringBuilder();
        AbstractTestResultAction<?> testResult = build.getAction(AbstractTestResultAction.class);

        if (null == testResult) {
            return "No tests ran.";
        }

        int failCount = testResult.getFailCount();

        if (failCount == 0) {
            buffer.append("All tests passed");
        } else {
            buffer.append(failCount).append(" tests failed.\n");

            boolean showOldFailures = !onlyRegressions;
            if(maxLength < Integer.MAX_VALUE) {
                maxLength *= 1024;
            }

            if (maxTests > 0) {
                int printedTests = 0;
                int printedLength = 0;
                for (TestResult failedTest : testResult.getFailedTests()) {
                    if (showOldFailures || getTestAge(failedTest) == 1) {
                        if (printedTests < maxTests && printedLength <= maxLength) {
                            printedLength += outputTest(buffer, failedTest, showStack, showMessage, maxLength-printedLength);
                            printedTests++;
                        }
                    }
                }
                if (failCount > printedTests) {
                    buffer.append("... and ").append(failCount - printedTests).append(" other failed tests.\n\n");
                }
                if (printedLength >= maxLength) {
                    buffer.append("\n\n... output truncated.\n\n");
                }
            }
        }

        return buffer.toString();
    }

    private int getTestAge(TestResult result) {
        if(result.isPassed())
            return 0;
        else if (result.getOwner() != null) {
            return result.getOwner().getNumber()-result.getFailedSince()+1;
        } else {
            return 0;
        }
    }

    private int outputTest(StringBuilder buffer, TestResult failedTest,
            boolean showStack, boolean showMessage, int lengthLeft) {
        StringBuilder local = new StringBuilder();
        
        local.append(failedTest.isPassed() ? "PASSED" : "FAILED").append(":  ");
        
        if(failedTest instanceof CaseResult) {
            local.append(((CaseResult)failedTest).getClassName());
        } else {
            local.append(failedTest.getFullName());
        }
        local.append('.').append(failedTest.getDisplayName()).append('\n');

        if (showMessage) {
            local.append("\nError Message:\n").append(failedTest.getErrorDetails()).append('\n');
        }
        
        if (showStack) {
            local.append("\nStack Trace:\n").append(failedTest.getErrorStackTrace()).append('\n');
        }

        if (showMessage || showStack) {
            local.append('\n');
        }

        if(local.length() > lengthLeft) {
            local.setLength(lengthLeft);
        }

        buffer.append(local.toString());
        return local.length();
    }
}
