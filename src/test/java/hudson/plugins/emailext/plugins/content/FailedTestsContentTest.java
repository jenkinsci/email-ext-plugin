package hudson.plugins.emailext.plugins.content;

import java.util.ArrayList;
import java.util.List;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.StreamTaskListener;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked"})
@RunWith(PowerMockRunner.class)
@PrepareForTest( { CaseResult.class })
public class FailedTestsContentTest
{
    private FailedTestsContent failedTestContent;

    private AbstractBuild build;
    
    private TaskListener listener;

    @Before
    public void setUp()
    {
        failedTestContent = new FailedTestsContent();
        listener = StreamTaskListener.fromStdout();

        build = mock( AbstractBuild.class );
    }

    @Test
    public void testGetContent_noTestsRanShouldGiveAMeaningfulMessage()
            throws Exception {
        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );

        assertEquals( "No tests ran.", content );
    }

    @Test
    public void testGetContent_whenAllTestsPassedShouldGiveMeaningfulMessage()
            throws Exception {
        AbstractTestResultAction testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 0 );

        when( build.getTestResultAction() ).thenReturn( testResults );

        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );

        assertEquals( "All tests passed", content );
    }

    @Test
    public void testGetContent_whenSomeTestsFailedShouldGiveMeaningfulMessage()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 123 );

        when( build.getTestResultAction() ).thenReturn( testResults );

        failedTestContent.maxTests = 0;
        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );

        assertEquals( "123 tests failed.\n", content );
    }

    @Test
    public void testGetContent_whenContentLargerThanMaxLengthShouldTruncate()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 5 );

        List<CaseResult> failedTests = new ArrayList<CaseResult>();
        for(int i = 0; i < 5; i++) {
            CaseResult result = mock( CaseResult.class );
            when( result.getStatus() ).thenReturn( CaseResult.Status.FAILED );
            when( result.getClassName() ).thenReturn( "hudson.plugins.emailext.ExtendedEmailPublisherTest" );
            when( result.getDisplayName() ).thenReturn( "Test" + i );
            when( result.getErrorDetails() ).thenReturn( StringUtils.leftPad( "", 3 * 1024, 'z' ) );
            when( result.getErrorStackTrace() ).thenReturn( StringUtils.leftPad( "", 200, 'e' ) );
            failedTests.add(result);
        }
        when( testResults.getFailedTests() ).thenReturn( failedTests );
        when( build.getTestResultAction() ).thenReturn( testResults );

        failedTestContent.maxLength = 10;
        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );
        assertTrue( content.length() < (3 * 1024 * 5) );

        failedTestContent = new FailedTestsContent();
        failedTestContent.showStack = true;
        content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );
        assertTrue( content.length() >= (3 * 1024 * 5) );
    }
}
