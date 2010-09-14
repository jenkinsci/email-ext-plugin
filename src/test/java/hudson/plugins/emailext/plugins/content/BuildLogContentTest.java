package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

public class BuildLogContentTest
{
    private BuildLogContent buildLogContent;

    private Map<String, Object> args;

    private AbstractBuild build;

    @Before
    public void setup()
    {
        buildLogContent = new BuildLogContent();

        args = new HashMap<String, Object>();

        build = mock(AbstractBuild.class);
    }

    @Test
    public void testGetContent_shouldConcatLogWithoutLineLimit()
            throws Exception
    {
        when(build.getLog(anyInt())).thenReturn(new LinkedList<String>()
        {{
                add("line 1");
                add("line 2");
                add("<line>3</line>");
            }});

        String content = buildLogContent.getContent(build, null, null, args);

        assertEquals("line 1\nline 2\n<line>3</line>\n", content);
    }

    @Test
    public void testGetContent_shouldTruncateWhenLineLimitIsHit()
            throws Exception
    {
        args.put(BuildLogContent.MAX_LINES_ARG_NAME, 2);

        buildLogContent.getContent(build, null, null, args);

        verify(build).getLog(2);
    }

    @Test
    public void testGetContent_shouldDefaultToMaxLines()
            throws Exception
    {
        buildLogContent.getContent(build, null, null, args);

        verify(build).getLog(BuildLogContent.MAX_LINES_DEFAULT_VALUE);
    }
}
