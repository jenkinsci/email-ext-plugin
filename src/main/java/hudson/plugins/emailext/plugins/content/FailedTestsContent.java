package hudson.plugins.emailext.plugins.content;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;

import java.util.ArrayList;
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
            try {
                return prepareYamlString(testResult, showStack, showMessage);
            } catch (Exception e) {
                //Could not convert to YAML
                e.printStackTrace();
            }
            return "BadTestFormat";
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

            setMaxLength();

            if (maxTests > 0) {
                int printedTests = 0;
                int printedLength = 0;
                for (TestResult failedTest : testResult.getFailedTests()) {
                    if (regressionFilter(failedTest)) {
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

    private boolean regressionFilter(TestResult failedTest) {
        return !onlyRegressions || getTestAge(failedTest) == 1;
    }

    private void setMaxLength() {
        if(maxLength < Integer.MAX_VALUE) {
            maxLength *= 1024;
        }
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

    private String prepareYamlString(AbstractTestResultAction<?> testResult, boolean showStack, boolean showMessage) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        SummarizedTestResult result = new SummarizedTestResult();
        if(null == testResult) {
            result.setSummary("No Tests ran");
            return om.writeValueAsString(result);
        }
        int failCount = testResult.getFailCount();
        if(failCount == 0) {
            result.setSummary("All tests passed");
        }
        else {
            result.setSummary(String.format("%d tests failed", failCount));
            setMaxLength();
            if(maxTests == 0) return om.writeValueAsString(result);
            int printSize = 0;
            for (TestResult failedTest : testResult.getFailedTests()) {
                if (regressionFilter(failedTest)) {
                    String stackTrace = showStack ? (escapeHtml ? escapeHtml(failedTest.getErrorStackTrace()) : failedTest.getErrorStackTrace()) : null;
                    String errorDetails = showMessage ? (escapeHtml ? escapeHtml(failedTest.getErrorDetails()) : failedTest.getErrorDetails()) : null;
                    String name = String.format( "%s.%s", (failedTest instanceof CaseResult) ? ((CaseResult)failedTest).getClassName() : failedTest.getFullName(),
                            failedTest.getDisplayName());
                    FailedTest t = new FailedTest(name, errorDetails, stackTrace);
                    String testYaml = om.writeValueAsString(t);
                    if(printSize <= maxLength && result.tests.size() < maxTests) {
                        result.addTest(t);
                        printSize += testYaml.length();
                    }
                }
            }
            result.setOtherFailedTests(failCount > result.tests.size());
            result.setTruncatedOutput(printSize > maxLength);
        }
        return om.writeValueAsString(result);
    }
    private String getLineBreak() {
        return escapeHtml ? "<br/>" : "\n";
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class FailedTest {
        String name;
        String errorMessage;
        String stackTrace;
        public FailedTest(String name, String errorMessage, String stackTrace){
            this.errorMessage = errorMessage;
            this.name = name;
            this.stackTrace = stackTrace;
        }

        @Override
        public String toString() {
            return String.format("Name:%s, Error message: %s, Stack trace:%s", this.name, this.errorMessage, this.stackTrace);
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class SummarizedTestResult {
        String summary;
        List<FailedTest> tests;
        boolean otherFailedTests;
        boolean truncatedOutput;

        public SummarizedTestResult() {
            this.tests = new ArrayList<FailedTest>();
        }
        public void setSummary(String summary) {
            this.summary = summary;
        }

        public void setOtherFailedTests(boolean otherFailedTests) {
            this.otherFailedTests = otherFailedTests;
        }

        public void setTruncatedOutput(boolean truncatedOutput) {
            this.truncatedOutput = truncatedOutput;
        }

        public void addTest(FailedTest t) {
            this.tests.add(t);
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for(FailedTest t: this.tests) {
                b.append(t.toString()).append("\n");
            }
            return String.format("Summary:%s, Tests: %s, Other failed tests:%s, Output truncated: %s",
                    this.summary, b.toString(), this.otherFailedTests, this.truncatedOutput);
        }
    }
}
