package hudson.plugins.emailext.plugins.content;

import static org.junit.Assert.*;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;

import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class BuildLogExcerptContentTest {

    private BuildLogExcerptContent buildLogExcerptContent;
    private TaskListener listener;

    @Before
    public void beforeTest() {
        buildLogExcerptContent = new BuildLogExcerptContent();
        listener = new StreamTaskListener(System.out);
    }

    @Test
    public void testGetContent_emptyBuildLogShouldStayEmpty()
            throws Exception {

        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getLogReader()).thenReturn(new StringReader(""));

        buildLogExcerptContent.start = "START";
        buildLogExcerptContent.end = "END";

        final String result = buildLogExcerptContent.evaluate(build, listener, BuildLogExcerptContent.MACRO_NAME);

        assertEquals("", result);
    }

    @Test
    public void testGetContent_simpleStartEndTags()
            throws Exception {

        AbstractBuild build = mock(AbstractBuild.class);

        when(build.getLogReader()).thenReturn(new StringReader("1\n2\n3\n4\n5\nSTART\n7\n8\n9\nEND\n10\n11\n12\n"));

        buildLogExcerptContent.start = "START";
        buildLogExcerptContent.end = "END";

        final String result = buildLogExcerptContent.evaluate(build, listener, BuildLogExcerptContent.MACRO_NAME);

        assertEquals("7\n8\n9\n", result);
    }

    @Test
    public void testGetContent_regexpStartEndTags()
            throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getLogReader()).thenReturn(new StringReader("1\n2\n3\n4\n5\nTEST STARTED\n7\n8\n9\nTEST STOPED\n10\n11\n12\n"));

        buildLogExcerptContent.start = ".*START.*";
        buildLogExcerptContent.end = ".*STOP.*";

        final String result = buildLogExcerptContent.evaluate(build, listener, BuildLogExcerptContent.MACRO_NAME);

        assertEquals("7\n8\n9\n", result);
    }
}
