package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class BuildLogContentTest {

    private AbstractBuild build;
    private TaskListener listener;
    private BuildLogContent buildLogContent;

    @Before
    public void setup() {
        build = mock(AbstractBuild.class);
        listener = StreamTaskListener.fromStdout();
        buildLogContent = new BuildLogContent();
    }

    @Test
    public void testGetContent_shouldConcatLogWithoutLineLimit()
            throws Exception {
        when(build.getLog(anyInt())).thenReturn(new LinkedList<String>() {
            {
                add("line 1");
                add("line 2");
                add("line 3");
            }
        });

        String content = buildLogContent.evaluate(build, build.getWorkspace(), listener, BuildLogContent.MACRO_NAME);

        assertEquals("line 1\nline 2\nline 3\n", content);
    }

    @Test
    public void testGetContent_shouldTruncateWhenLineLimitIsHit()
            throws Exception {
        buildLogContent.maxLines = 2;
        buildLogContent.evaluate(build, build.getWorkspace(), listener, BuildLogContent.MACRO_NAME);

        verify(build).getLog(2);
    }

    @Test
    public void testGetContent_shouldDefaultToMaxLines()
            throws Exception {
        buildLogContent.evaluate(build, build.getWorkspace(), listener, BuildLogContent.MACRO_NAME);

        verify(build).getLog(BuildLogContent.MAX_LINES_DEFAULT_VALUE);
    }

    @Test
    public void testGetContent_shouldDefaultToNotEscapeHtml()
            throws Exception {
        when(build.getLog(anyInt())).thenReturn(new LinkedList<String>() {
            {
                add("<b>bold</b>");
            }
        });

        String content = buildLogContent.evaluate(build, build.getWorkspace(), listener, BuildLogContent.MACRO_NAME);

        assertEquals("<b>bold</b>\n", content);
    }

    @Test
    public void testGetContent_shouldEscapeHtmlWhenArgumentEscapeHtmlSetToTrue()
            throws Exception {
        when(build.getLog(anyInt())).thenReturn(new LinkedList<String>() {
            {
                add("<b>bold</b>");
            }
        });

        buildLogContent.escapeHtml = true;
        String content = buildLogContent.evaluate(build, build.getWorkspace(), listener, BuildLogContent.MACRO_NAME);

        assertEquals("&lt;b&gt;bold&lt;/b&gt;\n", content);
    }
}
