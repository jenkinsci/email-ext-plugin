package hudson.plugins.emailext.plugins.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
    public boolean outputYaml = false;

    public static final String MACRO_NAME = "FAILED_TESTS";

    public static ObjectMapper om = new ObjectMapper(new YAMLFactory());

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

        if(outputYaml) {
            prepareYamlString(buffer, testResult, showStack, showMessage);
            return buffer.toString();
        }
        if (null == testResult) {
            return "No tests ran.";
        }

        int failCount = testResult.getFailCount();

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
                for (TestResult failedTest : testResult.getFailedTests()) {
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

    private void getSummaryText(StringBuilder buffer, AbstractTestResultAction<?> testResult) {
        if(testResult == null) {

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

    private void prepareYamlString(StringBuilder buffer, AbstractTestResultAction<?> testResult, boolean showStack, boolean showMessage){
        SummarizedTestResult result = new SummarizedTestResult();
        if(null == testResult) {
            result.summaryString = "No Tests ran";
            buffer.append(getYamlString(result));
            return;
        }
        int failCount = testResult.getFailCount();
        if(failCount == 0) {
            result.summaryString = "All tests passed";
        }
        else {

        }
    }
    private String getLineBreak() {
        return escapeHtml ? "<br/>" : "\n";
    }

    private String getYamlString(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            //Return an empty string
        }
        return "";
    }

    private class FailedTest {
        String name;
        String errorMessage;
        String stackTrace;
        public FailedTest(String name, String errorMessage, String stackTrace){
            this.errorMessage = errorMessage;
            this.name = name;
            this.stackTrace = stackTrace;
        }
    }
    private class SummarizedTestResult {
        public String summaryString;
        public List<FailedTest> tests;
        public boolean otherFailedTests;
        public boolean truncatedOutPut;
        public SummarizedTestResult() {
            this.tests.clear();
            this.truncatedOutPut = false;
            this. otherFailedTests = false;
        }
    }
}
