package hudson.plugins.emailext.plugins.content;

import java.util.Collections;
import hudson.tasks.test.AbstractTestResultAction;
import java.io.IOException;
import hudson.model.AbstractBuild;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for TestCountsContentTest.
 *
 * @author Seiji Sogabe
 */
public class TestCountsContentTest {

    private TestCountsContent target;

    private AbstractBuild<?, ?> build;

    @Before
    public void setUp() {
        target = new TestCountsContent();
        build = mock(AbstractBuild.class);
    }

    @After
    public void tearDown() {
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetContent_NoTestResults() throws IOException, InterruptedException {
        assertEquals("", target.getContent(build, null, null, Collections.singletonMap("var", "total")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetContent() throws IOException, InterruptedException {
        AbstractTestResultAction<?> results = mock(AbstractTestResultAction.class);
        when(results.getTotalCount()).thenReturn(5);
        when(results.getFailCount()).thenReturn(2);
        when(results.getSkipCount()).thenReturn(1);
        when(build.getTestResultAction()).thenReturn(results);

        assertEquals("5", target.getContent(build, null, null, Collections.EMPTY_MAP));
        assertEquals("5", target.getContent(build, null, null, Collections.singletonMap("var", "total")));
        assertEquals("2", target.getContent(build, null, null, Collections.singletonMap("var", "fail")));
        assertEquals("1", target.getContent(build, null, null, Collections.singletonMap("var", "skip")));

        assertEquals("1", target.getContent(build, null, null, Collections.singletonMap("var", "SKIP")));

        assertEquals("", target.getContent(build, null, null, Collections.singletonMap("var", "wrongvar")));
    }
}
