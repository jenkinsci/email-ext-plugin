package hudson.plugins.emailext.plugins.content;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScriptContentBuildWrapperTest {
    private ScriptContentBuildWrapper buildWrapper;

    private AbstractBuild<?, ?> mockBuild;

    @BeforeEach
    void setup() {
        mockBuild = mock(AbstractBuild.class);

        buildWrapper = new ScriptContentBuildWrapper(mockBuild);
    }

    @Test
    void testGetTimestampString() {
        final Calendar calendar = Calendar.getInstance();
        when(mockBuild.getTimestamp()).thenReturn(calendar);

        assertEquals(Functions.rfc822Date(calendar), buildWrapper.getTimestampString());
    }

    @Test
    void testGetAction_whenActionNotFoundThenReturnNull() {
        when(mockBuild.getActions()).thenReturn(new LinkedList<>());

        assertNull(buildWrapper.getAction("class.not.found"));
    }

    @Test
    void testGetJUnitTestResult_whenMavenProjectUseMavenPluginsSurefireAggregatedReport() {
        final AggregatedTestResultAction surefireAggregatedReport = mock(AggregatedTestResultAction.class);
        final ChildReport childReport1 = mockChildReport();
        final ChildReport childReport2 = mockChildReport();
        when(surefireAggregatedReport.getChildReports()).thenReturn(new LinkedList<>() {
            {
                add(childReport1);
                add(childReport2);
            }
        });

        when(mockBuild.getActions(AggregatedTestResultAction.class)).thenReturn(new LinkedList<>() {
            {
                add(surefireAggregatedReport);
            }
        });

        final List<TestResult> testResults = buildWrapper.getJUnitTestResult();

        assertEquals(2, testResults.size());
        assertSame(childReport1.result, testResults.get(0));
        assertSame(childReport2.result, testResults.get(1));
    }

    private ChildReport mockChildReport() {
        final AbstractTestResultAction<?> testResultAction1 = mock(AbstractTestResultAction.class);
        when(testResultAction1.getResult()).thenReturn(new TestResult());
        return new ChildReport(mockBuild, testResultAction1);
    }

    @Test
    void testGetJUnitTestResult_listShouldBeEmptyWhenNoTestsFound() {
        final List<TestResult> testResults = buildWrapper.getJUnitTestResult();

        assertTrue(testResults.isEmpty());
    }

    @Test
    void testGetJUnitTestResult_whenFreestyleProjectShouldGetTest() {
        final TestResult testResult = new TestResult();
        final TestResultAction testResultAction = mock(TestResultAction.class);

        when(testResultAction.getResult()).thenReturn(testResult);
        when(mockBuild.getAction(TestResultAction.class)).thenReturn(testResultAction);

        final List<TestResult> testResults = buildWrapper.getJUnitTestResult();

        assertEquals(1, testResults.size());
        assertSame(testResult, testResults.get(0));
    }
}
