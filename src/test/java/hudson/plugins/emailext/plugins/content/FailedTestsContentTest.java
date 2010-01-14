package hudson.plugins.emailext.plugins.content;

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
}
