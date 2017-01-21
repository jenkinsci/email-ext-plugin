package hudson.plugins.emailext.plugins.content;

import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( PowerMockRunner.class )
@PrepareForTest( value = { AggregatedTestResultAction.class, AggregatedTestResultAction.ChildReport.class } )
public class ScriptContentBuildWrapperTest
{
    private ScriptContentBuildWrapper buildWrapper;

    private AbstractBuild<?, ?> mockBuild;

    @Before
    public void setup()
    {
        mockBuild = mock( AbstractBuild.class );

        buildWrapper = new ScriptContentBuildWrapper( mockBuild );
    }

    @Test
    public void testGetTimestampString()
    {
        final Calendar calendar = Calendar.getInstance();
        when( mockBuild.getTimestamp() ).thenReturn( calendar );

        assertEquals( Functions.rfc822Date( calendar ), buildWrapper.getTimestampString() );
    }

    @Test
    public void testGetAction_whenActionNotFoundThenReturnNull()
    {
        when( mockBuild.getActions() ).thenReturn( new LinkedList<Action>() );

        assertNull( buildWrapper.getAction( "class.not.found" ) );
    }

    @Test
    public void testGetAction_returnActionWhenFoundByName()
    {
        final Action expectedAction = mock( Action.class );
        when( mockBuild.getActions() ).thenReturn( new LinkedList<Action>()
            {{
                add( expectedAction );
            }} );

        assertSame( expectedAction, buildWrapper.getAction( expectedAction.getClass().getName() ) );
    }

    @Test
    public void testGetJUnitTestResult_whenMavenProjectUseMavenPluginsSurefireAggregatedReport()
    {
        final AggregatedTestResultAction surefireAggregatedReport = mock( AggregatedTestResultAction.class );
        final ChildReport childReport1 = mockChildReport();
        final ChildReport childReport2 = mockChildReport();
        when( surefireAggregatedReport.getChildReports() ).thenReturn(
            new LinkedList<AggregatedTestResultAction.ChildReport>()
            {{
                    add( childReport1 );
                    add( childReport2 );
                }} );

        when( mockBuild.getActions(AggregatedTestResultAction.class) ).thenReturn( new LinkedList<AggregatedTestResultAction>()
        {{
                add( surefireAggregatedReport );
            }} );

        final List<TestResult> testResults = buildWrapper.getJUnitTestResult();

        assertEquals( 2, testResults.size() );
        assertSame( childReport1.result, testResults.get( 0 ) );
        assertSame( childReport2.result, testResults.get( 1 ) );
    }

    private ChildReport mockChildReport()
    {
        final AbstractTestResultAction testResultAction1 = mock( AbstractTestResultAction.class );
        when( testResultAction1.getResult() ).thenReturn( new TestResult() );
        return new ChildReport( mockBuild, testResultAction1 );
    }

    @Test
    public void testGetJUnitTestResult_listShouldBeEmptyWhenNoTestsFound()
    {
        final List<TestResult> testResults = buildWrapper.getJUnitTestResult();

        assertTrue( testResults.isEmpty() );
    }

    @Test
    public void testGetJUnitTestResult_whenFreestyleProjectShouldGetTest()
    {
        final TestResult testResult = new TestResult();
        final TestResultAction testResultAction = mock( TestResultAction.class );

        when( testResultAction.getResult() ).thenReturn( testResult );
        when( mockBuild.getAction( TestResultAction.class ) ).thenReturn( testResultAction );

        final List<TestResult> testResults = buildWrapper.getJUnitTestResult();

        assertEquals( 1, testResults.size() );
        assertSame( testResult, testResults.get( 0 ) );
    }
}
