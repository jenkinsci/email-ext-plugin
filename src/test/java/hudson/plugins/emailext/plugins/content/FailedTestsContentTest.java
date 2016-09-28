package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import hudson.util.StreamTaskListener;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SuppressWarnings({"unchecked"})
@RunWith(PowerMockRunner.class)
@PrepareForTest( { TestResult.class })
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

    /**
     * Verifies that token expansion works for pipeline builds (JENKINS-38519).
     */
    @Test
    public void testGetContent_withWorkspaceAndNoTestsRanShouldGiveAMeaningfulMessage()
            throws Exception {
        String content = failedTestContent.evaluate( build, build.getWorkspace(), listener, FailedTestsContent.MACRO_NAME );

        assertEquals( "No tests ran.", content );
    }

    @Test
    public void testGetContent_whenAllTestsPassedShouldGiveMeaningfulMessage()
            throws Exception {
        AbstractTestResultAction testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 0 );

        when( build.getAction(AbstractTestResultAction.class) ).thenReturn( testResults );

        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );

        assertEquals( "All tests passed", content );
    }

    @Test
    public void testGetContent_whenSomeTestsFailedShouldGiveMeaningfulMessage()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 123 );

        when( build.getAction(AbstractTestResultAction.class) ).thenReturn( testResults );

        failedTestContent.maxTests = 0;
        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );

        assertEquals( "123 tests failed.\n", content );
    }

    @Test
    public void testGetContent_withMessage_withStack()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 2 );

        List<TestResult> failedTests = new ArrayList<TestResult>();
        for(int i = 0; i < 2; i++) {
            TestResult result = mock( TestResult.class );
            when( result.isPassed() ).thenReturn( false );
            when( result.getFullName() ).thenReturn( "hudson.plugins.emailext.ExtendedEmailPublisherTest" );
            when( result.getDisplayName() ).thenReturn( "Test" + i );
            when( result.getErrorDetails() ).thenReturn( "Error" + i );
            when( result.getErrorStackTrace() ).thenReturn( "Stack" + i );
            failedTests.add(result);
        }
        
        Mockito.<List<? extends TestResult>>when( testResults.getFailedTests() ).thenReturn( failedTests );        
        when( build.getAction(AbstractTestResultAction.class) ).thenReturn( testResults );

        failedTestContent.maxTests = 2;
        failedTestContent.showMessage = true;
        failedTestContent.showStack = true;
        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );

        for(int i = 0; i < 2; i++) {
            assertTrue( content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i) );
            assertTrue( content.contains("Error Message:\nError" + i) );
            assertTrue( content.contains("Stack Trace:\nStack" + i) );
        }
    }

    @Test
    public void testGetContent_noMessage_withStack()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 2 );

        List<TestResult> failedTests = new ArrayList<TestResult>();
        for(int i = 0; i < 2; i++) {
            TestResult result = mock( TestResult.class );
            when( result.isPassed() ).thenReturn( false );
            when( result.getFullName() ).thenReturn( "hudson.plugins.emailext.ExtendedEmailPublisherTest" );
            when( result.getDisplayName() ).thenReturn( "Test" + i );
            when( result.getErrorDetails() ).thenReturn( "Error" + i );
            when( result.getErrorStackTrace() ).thenReturn( "Stack" + i );
            failedTests.add(result);
        }
        Mockito.<List<? extends TestResult>>when( testResults.getFailedTests() ).thenReturn( failedTests );
        when( build.getAction(AbstractTestResultAction.class) ).thenReturn( testResults );

        failedTestContent.maxTests = 2;
        failedTestContent.showMessage = false;
        failedTestContent.showStack = true;
        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );

        for(int i = 0; i < 2; i++) {
            assertTrue( content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i) );
            assertFalse( content.contains("Error Message:\nError" + i) );
            assertTrue( content.contains("Stack Trace:\nStack" + i) );
        }
    }

    @Test
    public void testGetContent_withMessage_noStack()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 2 );

        List<TestResult> failedTests = new ArrayList<TestResult>();
        for(int i = 0; i < 2; i++) {
            TestResult result = mock( TestResult.class );
            when( result.isPassed() ).thenReturn( false );
            when( result.getFullName() ).thenReturn( "hudson.plugins.emailext.ExtendedEmailPublisherTest" );
            when( result.getDisplayName() ).thenReturn( "Test" + i );
            when( result.getErrorDetails() ).thenReturn( "Error" + i );
            when( result.getErrorStackTrace() ).thenReturn( "Stack" + i );
            failedTests.add(result);
        }
        Mockito.<List<? extends TestResult>>when( testResults.getFailedTests() ).thenReturn( failedTests );
        when( build.getAction(AbstractTestResultAction.class) ).thenReturn( testResults );

        failedTestContent.maxTests = 2;
        failedTestContent.showMessage = true;
        failedTestContent.showStack = false;
        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );

        for(int i = 0; i < 2; i++) {
            assertTrue( content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i) );
            assertTrue( content.contains("Error Message:\nError" + i) );
            assertFalse( content.contains("Stack Trace:\nStack" + i) );
        }
    }

    @Test
    public void testGetContent_noMessage_noStack()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 2 );

        List<TestResult> failedTests = new ArrayList<TestResult>();
        for(int i = 0; i < 2; i++) {
            TestResult result = mock( TestResult.class );
            when( result.isPassed() ).thenReturn( false );
            when( result.getFullName() ).thenReturn( "hudson.plugins.emailext.ExtendedEmailPublisherTest" );
            when( result.getDisplayName() ).thenReturn( "Test" + i );
            when( result.getErrorDetails() ).thenReturn( "Error" + i );
            when( result.getErrorStackTrace() ).thenReturn( "Stack" + i );
            failedTests.add(result);
        }
        Mockito.<List<? extends TestResult>>when( testResults.getFailedTests() ).thenReturn( failedTests );
        when( build.getAction(AbstractTestResultAction.class) ).thenReturn( testResults );

        failedTestContent.maxTests = 2;
        failedTestContent.showMessage = false;
        failedTestContent.showStack = false;
        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );

        for(int i = 0; i < 2; i++) {
            assertTrue( content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i) );
            assertFalse( content.contains("Error Message:\nError" + i) );
            assertFalse( content.contains("Stack Trace:\nStack" + i) );
        }
    }

    @Test
    public void testGetContent_whenContentLargerThanMaxLengthShouldTruncate()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock( AbstractTestResultAction.class );
        when( testResults.getFailCount() ).thenReturn( 5 );

        List<TestResult> failedTests = new ArrayList<TestResult>();
        for(int i = 0; i < 5; i++) {
            TestResult result = mock( TestResult.class );
            when( result.isPassed() ).thenReturn( false );
            when( result.getFullName() ).thenReturn( "hudson.plugins.emailext.ExtendedEmailPublisherTest" );
            when( result.getDisplayName() ).thenReturn( "Test" + i );
            when( result.getErrorDetails() ).thenReturn( StringUtils.leftPad( "", 3 * 1024, 'z' ) );
            when( result.getErrorStackTrace() ).thenReturn( StringUtils.leftPad( "", 200, 'e' ) );
            failedTests.add(result);
        }

        Mockito.<List<? extends TestResult>>when( testResults.getFailedTests() ).thenReturn( failedTests );
        when( build.getAction(AbstractTestResultAction.class) ).thenReturn( testResults );

        failedTestContent.maxLength = 10;
        String content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );
        assertTrue( content.length() < (3 * 1024 * 5) );

        failedTestContent = new FailedTestsContent();
        failedTestContent.showStack = true;
        content = failedTestContent.evaluate( build, listener, FailedTestsContent.MACRO_NAME );
        assertTrue( content.length() >= (3 * 1024 * 5) );
    }
}
