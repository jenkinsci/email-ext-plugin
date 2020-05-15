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
        AbstractTestResultAction<?> testResult = run.getAction(AbstractTestResultAction.class);
        SummarizedTestResult result = prepareSummarizedTestResult(testResult, showStack, showMessage);

        if(outputYaml) {
            try {
                return result.toYamlString();
            } catch (Exception e) {

            }
            return "Bad format";
        }
        return result.toString(getLineBreak(), showStack, showMessage);
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

    private SummarizedTestResult prepareSummarizedTestResult(AbstractTestResultAction<?> testResult, boolean showStack, boolean showMessage) {
        if(null == testResult) {
            SummarizedTestResult result = new SummarizedTestResult(0);
            result.setSummary("No tests ran.");
            return result;
        }
        int failCount = testResult.getFailCount();
        SummarizedTestResult result = new SummarizedTestResult(failCount);
        if(failCount == 0) {
            result.setSummary("All tests passed");
        }
        else {
            result.setSummary(String.format("%d tests failed.", failCount));
            setMaxLength();
            if(maxTests == 0) return result;
            int printSize = 0;
            for (TestResult failedTest : testResult.getFailedTests()) {
                if (regressionFilter(failedTest)) {
                    String stackTrace = showStack ? (escapeHtml ? escapeHtml(failedTest.getErrorStackTrace()) : failedTest.getErrorStackTrace()) : null;
                    String errorDetails = showMessage ? (escapeHtml ? escapeHtml(failedTest.getErrorDetails()) : failedTest.getErrorDetails()) : null;
                    String name = String.format( "%s.%s", (failedTest instanceof CaseResult) ? ((CaseResult)failedTest).getClassName() : failedTest.getFullName(),
                            failedTest.getDisplayName());
                    FailedTest t = new FailedTest(name, failedTest.isPassed(), errorDetails, stackTrace);
                    String testYaml = t.toString();
                    if(printSize <= maxLength && result.tests.size() < maxTests) {
                        result.tests.add(t);
                        printSize += testYaml.length();
                    }
                }
            }
            result.otherFailedTests = (failCount > result.tests.size());
            result.truncatedOutput = (printSize > maxLength);
        }
        return result;
    }
    private String getLineBreak() {
        return escapeHtml ? "<br/>" : "\n";
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class FailedTest {
        String name;
        String status;
        String errorMessage;
        String stackTrace;
        public FailedTest(String name, boolean status, String errorMessage, String stackTrace){
            this.errorMessage = errorMessage;
            this.name = name;
            this.stackTrace = stackTrace;
            this.status = status ? "PASSED" : "FAILED";
        }

        @Override
        public String toString() {
            return String.format("Name:%s, Status:%s, Error message: %s, Stack trace:%s", this.name, this.status, this.errorMessage, this.stackTrace);
        }
    }

    private static class SummarizedTestResult {
        public String summary;
        public List<FailedTest> tests;
        public boolean otherFailedTests;
        public boolean truncatedOutput;
        private int totalFailCount;

        public SummarizedTestResult(int failCount) {
            this.tests = new ArrayList<FailedTest>();
            this.totalFailCount = failCount;
        }
        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String toString(String lineBreak, boolean showStack, boolean showMessage) {
            StringBuilder builder = new StringBuilder();
            builder.append(this.summary);
            if(this.tests.size() == 0) return builder.toString();
            builder.append(lineBreak);
            for(FailedTest t: this.tests) {
                outputTest(builder, lineBreak, t, showStack, showMessage);
            }
            if (this.otherFailedTests) {
                builder.append("... and ").append(totalFailCount - tests.size()).append(" other failed tests.")
                        .append(lineBreak);
            }
            if (this.truncatedOutput) {
                builder.append(lineBreak).append("... output truncated.").append(lineBreak);
            }

            return builder.toString();
        }

        public String toYamlString() throws JsonProcessingException {
            ObjectMapper om = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
            return om.writeValueAsString(this);
        }

        private void outputTest(StringBuilder buffer, String lineBreak, FailedTest failedTest,
                               boolean showStack, boolean showMessage) {
            StringBuilder local = new StringBuilder();
            local.append(failedTest.status).append(":  ");

            local.append(failedTest.name).append(lineBreak);

            if (showMessage) {
                local.append(lineBreak).append("Error Message:").append(lineBreak).append(failedTest.errorMessage).append(lineBreak);
            }

            if (showStack) {
                local.append(lineBreak).append("Stack Trace:").append(lineBreak).append(failedTest.stackTrace).append(lineBreak);
            }

            if (showMessage || showStack) {
                local.append(lineBreak);
            }
            buffer.append(local.toString());
        }
    }
}
