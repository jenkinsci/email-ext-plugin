package hudson.plugins.emailext.plugins.content;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

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

    @Parameter
    public boolean escapeHtml = false;

    @Parameter
    public String testNameRegexPattern = "";

    public static final String MACRO_NAME = "FAILED_TESTS";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }



    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName) {
        return evaluate(build, build.getWorkspace(), listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName) {
        StringBuilder buffer = new StringBuilder();
        AbstractTestResultAction<?> testResult = run.getAction(AbstractTestResultAction.class);

        if (null == testResult) {
            return "No tests ran.";
        }

        int failCount = testResult.getFailCount();

        List<TestResult> failedAndFilteredTests = filterTests(testResult.getFailedTests(), testNameRegexPattern);
        failCount = testNameRegexPattern.length() == 0 ? failCount : failedAndFilteredTests.size();

        if (failCount == 0) {
            buffer.append("All tests passed");
        } else {
            String lineBreak = getLineBreak();
            buffer.append(failCount).append(" tests failed.").append(lineBreak);

            boolean showOldFailures = !onlyRegressions;
            if(maxLength < Integer.MAX_VALUE) {
                maxLength *= 1024;
            }

            if (maxTests > 0) {
                int printedTests = 0;
                int printedLength = 0;
                for (TestResult failedTest : failedAndFilteredTests) {
                    if (showOldFailures || getTestAge(failedTest) == 1) {
                        if (printedTests < maxTests && printedLength <= maxLength) {
                            printedLength += outputTest(buffer, failedTest, showStack, showMessage, maxLength-printedLength);
                            printedTests++;
                        }
                    }
                }
                if (failCount > printedTests) {
                    buffer.append("... and ").append(failCount - printedTests).append(" other failed tests.")
                        .append(lineBreak);
                }
                if (printedLength >= maxLength) {
                    buffer.append(lineBreak).append("... output truncated.").append(lineBreak);
                }
            }
        }

        return buffer.toString();
    }

    private int getTestAge(TestResult result) {
        if(result.isPassed())
            return 0;
        else if (result.getRun() != null) {
            return result.getRun().getNumber()-result.getFailedSince()+1;
        } else {
            return 0;
        }
    }

    private int outputTest(StringBuilder buffer, TestResult failedTest,
            boolean showStack, boolean showMessage, int lengthLeft) {
        StringBuilder local = new StringBuilder();
        String lineBreak = getLineBreak();

        local.append(failedTest.isPassed() ? "PASSED" : "FAILED").append(":  ");

        if(failedTest instanceof CaseResult) {
            local.append(((CaseResult)failedTest).getClassName());
        } else {
            local.append(failedTest.getFullName());
        }
        local.append('.').append(failedTest.getDisplayName()).append(lineBreak);

        if (showMessage) {
            String errorDetails = escapeHtml ? escapeHtml(failedTest.getErrorDetails()) : failedTest.getErrorDetails();
            local.append(lineBreak).append("Error Message:").append(lineBreak).append(errorDetails).append(lineBreak);
        }

        if (showStack) {
            String stackTrace = escapeHtml ? escapeHtml(failedTest.getErrorStackTrace()) : failedTest.getErrorStackTrace();
            local.append(lineBreak).append("Stack Trace:").append(lineBreak).append(stackTrace).append(lineBreak);
        }

        if (showMessage || showStack) {
            local.append(lineBreak);
        }

        if(local.length() > lengthLeft) {
            local.setLength(lengthLeft);
        }

        buffer.append(local.toString());
        return local.length();
    }

    private String getLineBreak() {
        return escapeHtml ? "<br/>" : "\n";
    }

    private List<TestResult> filterTests(List<? extends TestResult> failedTests, String regexPattern) {
        List<TestResult> filteredTests = failedTests.stream().collect(Collectors.toList());
        if(regexPattern.length() != 0) {
            Pattern pattern = Pattern.compile(regexPattern);
            filteredTests = filteredTests.stream().filter(t -> pattern.matcher(t.getFullName()).matches()).collect(Collectors.toList());
        }
        return filteredTests;
    }
}
