package hudson.plugins.emailext.plugins.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
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

    @Before
    public void setUp()
    {
        failedTestContent = new FailedTestsContent();

        build = mock( AbstractBuild.class );
    }

    @Test
    public void testGetContent_noTestsRanShouldGiveAMeaningfulMessage()
    {
        String content = failedTestContent.getContent( build, null, null, null );

        assertEquals( "No tests ran.", content );
    }

    @Test
    public void testGetContent_whenAllTestsPassedShouldGiveMeaningfulMessage()
    {
        AbstractTestResultAction testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 0 );

        when( build.getTestResultAction() ).thenReturn( testResults );

        String content = failedTestContent.getContent( build, null, null, null );

        assertEquals( "All tests passed", content );
    }

    @Test
    public void testGetContent_whenSomeTestsFailedShouldGiveMeaningfulMessage()
    {
        AbstractTestResultAction<?> testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 123 );

        when( build.getTestResultAction() ).thenReturn( testResults );

        Map<String, Integer> args = Collections.singletonMap(
                FailedTestsContent.MAX_TESTS_ARG_NAME, 0 );
        String content = failedTestContent.getContent( build, null, null, args );

        assertEquals( "123 tests failed.\n", content );
    }

    @Test
    public void testGetContent_whenContentLargerThanMaxLengthShouldTruncate()
    {
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

        Map<String, Integer> args = Collections.singletonMap(
                FailedTestsContent.MAX_LENGTH_ARG_NAME, 10 );
        String content = failedTestContent.getContent( build, null, null, args );
        assertTrue( content.length() < (3 * 1024 * 5) );

        Map<String, Boolean> args2 = Collections.singletonMap(
            "showStack", true);
        content = failedTestContent.getContent( build, null, null, args2 );
        assertTrue( content.length() >= (3 * 1024 * 5) );
    }
}
