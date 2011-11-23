package hudson.plugins.emailext.plugins.content;

import java.util.Collections;
import java.util.Map;

import hudson.model.AbstractBuild;
import hudson.tasks.test.AbstractTestResultAction;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked"})
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

}
