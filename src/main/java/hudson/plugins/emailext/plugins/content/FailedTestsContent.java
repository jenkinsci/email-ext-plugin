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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringEscapeUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

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
    public String outputFormat = "";

    @Parameter
    public String testNamePattern = "";

    public static final String MACRO_NAME = "FAILED_TESTS";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }


    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName) throws MacroEvaluationException{
        return evaluate(build, build.getWorkspace(), listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName) throws MacroEvaluationException {
        AbstractTestResultAction<?> testResult = run.getAction(AbstractTestResultAction.class);
        SummarizedTestResult result = prepareSummarizedTestResult(testResult);

        if ("yaml".equals(outputFormat)) {
            try {
                return result.toYamlString();
            } catch (JsonProcessingException e) {
                throw new MacroEvaluationException("Unable to serialize to yaml", MACRO_NAME, e.getCause());
            }
        }
        return result.toString();
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

    private SummarizedTestResult prepareSummarizedTestResult(AbstractTestResultAction<?> testResult) {
        if(null == testResult) {
            SummarizedTestResult result = new SummarizedTestResult(0, getLineBreak());
            result.summary = "No tests ran.";
            return result;
        }
        int failCount = testResult.getFailCount();
        List<? extends TestResult> failedAndFilteredTests = filterTests(testResult.getFailedTests(), testNamePattern);
        failCount = testNamePattern.length() == 0 ? failCount : failedAndFilteredTests.size();
        SummarizedTestResult result = new SummarizedTestResult(failCount, getLineBreak());
        if(failCount == 0) {
            result.summary = "All tests passed";
        }
        else {
            result.summary = String.format("%d tests failed.", failCount);
            setMaxLength();
            if(maxTests > 0) {
                int printSize = 0;
                for (TestResult failedTest : failedAndFilteredTests) {
                    if (regressionFilter(failedTest)) {
                        printSize = addTest(result, printSize, failedTest);
                    }
                }
                result.otherFailedTests = (failCount > result.tests.size());
                result.truncatedOutput = (printSize > maxLength);
            }
        }
        return result;
    }

    private int addTest(SummarizedTestResult result, int printSize, TestResult failedTest) {
        String stackTrace = showStack ? (escapeHtml ? StringEscapeUtils.escapeHtml(failedTest.getErrorStackTrace()) : failedTest.getErrorStackTrace()) : null;
        String errorDetails = showMessage ? (escapeHtml ? StringEscapeUtils.escapeHtml(failedTest.getErrorDetails()) : failedTest.getErrorDetails()) : null;
        String name = String.format("%s.%s", (failedTest instanceof CaseResult) ? ((CaseResult) failedTest).getClassName() : failedTest.getFullName(),
                failedTest.getDisplayName());
        FailedTest t = new FailedTest(name, failedTest.isPassed(), errorDetails, stackTrace);
        String testYaml = t.toString();
        if (printSize <= maxLength && result.tests.size() < maxTests) {
            result.tests.add(t);
            printSize += testYaml.length();
        }
        return printSize;
    }

    public String getLineBreak() {
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

    @Override
    public boolean handlesHtmlEscapeInternally() {
        return true;
    }

    private static class SummarizedTestResult {
        public String summary;
        public List<FailedTest> tests;
        public boolean otherFailedTests;
        public boolean truncatedOutput;
        private int totalFailCount;
        private String lineBreak;

        public SummarizedTestResult(int failCount, String lineBreak) {
            this.tests = new ArrayList<>();
            this.totalFailCount = failCount;
            this.lineBreak = lineBreak;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(this.summary);
            if (!tests.isEmpty()) {
                builder.append(lineBreak);
            }
            for(FailedTest t: tests) {
                outputTest(builder, lineBreak, t);
            }
            if (otherFailedTests) {
                builder.append("... and ").append(totalFailCount - tests.size()).append(" other failed tests.")
                        .append(lineBreak);
            }
            if (this.truncatedOutput) {
                builder.append(lineBreak).append("... output truncated.").append(lineBreak);
            }

            return builder.toString();
        }

        public String toYamlString() throws JsonProcessingException {
            ObjectMapper om = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true));
            return om.writeValueAsString(this);
        }

        private void outputTest(StringBuilder buffer, String lineBreak, FailedTest failedTest) {
            StringBuilder local = new StringBuilder();
            local.append(failedTest.status).append(":  ");

            local.append(failedTest.name).append(lineBreak);

            if (failedTest.errorMessage != null) {
                local.append(lineBreak).append("Error Message:").append(lineBreak).append(failedTest.errorMessage).append(lineBreak);
            }

            if (failedTest.stackTrace != null) {
                local.append(lineBreak).append("Stack Trace:").append(lineBreak).append(failedTest.stackTrace).append(lineBreak);
            }

            if (failedTest.stackTrace != null || failedTest.errorMessage != null) {
                local.append(lineBreak);
            }
            buffer.append(local);
        }

    }

    private List<? extends TestResult> filterTests(List<? extends TestResult> failedTests, String regexPattern) {
        if(regexPattern.length() != 0) {
            Pattern pattern = Pattern.compile(regexPattern);
            failedTests = failedTests.stream().filter(t -> pattern.matcher(t.getFullName()).matches()).collect(Collectors.toList());
        }
        return failedTests;
    }
}
