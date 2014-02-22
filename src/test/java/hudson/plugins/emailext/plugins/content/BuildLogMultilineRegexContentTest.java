package hudson.plugins.emailext.plugins.content;

import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class BuildLogMultilineRegexContentTest {

    private BuildLogMultilineRegexContent buildLogMultilineRegexContent;
    private AbstractBuild build;
    private TaskListener listener;

    @Before
    public void beforeTest() {
        buildLogMultilineRegexContent = new BuildLogMultilineRegexContent();
        buildLogMultilineRegexContent.regex = ".+";
        build = mock(AbstractBuild.class);
        listener = StreamTaskListener.fromStdout();
    }

    @Test
    public void testGetContent_multilineDotallRegex() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
        buildLogMultilineRegexContent.regex = "(?s)start:.*end\\.";
        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);
        assertEquals(
                "[...truncated 2 lines...]\nstart:\r\na\r\nb\r\nc\r\nend.\n[...truncated 3 lines...]\n",
                result);
    }

    @Test
    public void testGetContent_multilineDotallRegex2() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
        buildLogMultilineRegexContent.regex = "rt:(?s:.*)en";
        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);
        assertEquals("[...truncated 2 lines...]\nrt:\r\na\r\nb\r\nc\r\nen\n[...truncated 4 lines...]\n",
                result);
    }

    @Test
    public void testGetContent_multilineEOLRegex() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
        buildLogMultilineRegexContent.regex = "start:\\r?\\n(.*\\r?\\n)+end\\.";
        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);
        assertEquals("[...truncated 2 lines...]\nstart:\r\na\r\nb\r\nc\r\nend.\n[...truncated 3 lines...]\n",
                result);
    }

    @Test
    public void testGetContent_multilineCommentsAlternationsRegex() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
        buildLogMultilineRegexContent.regex = "(?x)\n"
                + "# first alternative\n"
                + "line\\ \\#1(?s:.*)\\#2\n"
                + "# second alternative\n"
                + "|start:(?s:.*)end\\."
                + "# third alternative"
                + "|xyz(?s:.*)omega";
        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);
        assertEquals("line #1\r\nline #2\nstart:\r\na\r\nb\r\nc\r\nend.\n[...truncated 3 lines...]\n",
                result);
    }

    @Test
    public void testGetContent_emptyBuildLogShouldStayEmpty()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(""));
        final BufferedReader reader = new BufferedReader(new StringReader(""));
        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);
        assertEquals("", result);
    }

    @Test
    public void testGetContent_matchedLines()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        buildLogMultilineRegexContent.regex = ".*(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b.*";
        buildLogMultilineRegexContent.showTruncatedLines = false;
        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);
        assertEquals("6 ERROR\n9 ERROR\n18 ERROR\n", result);
    }

    @Test
    public void testGetContent_truncatedAndMatchedLines()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));

        buildLogMultilineRegexContent.regex = ".*(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b.*";
        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);

        assertEquals("[...truncated 5 lines...]\n6 ERROR\n[...truncated 2 lines...]\n9 ERROR\n[...truncated 8 lines...]\n18 ERROR\n[...truncated 5 lines...]\n", result);
    }

    @Test
    public void testGetContent_errorMatchedAndNothingReplaced()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar fubber"));
        buildLogMultilineRegexContent.substText = "$0";

        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);

        assertEquals("error foo bar fubber\n", result);
    }

    @Test
    public void testGetContent_errorMatchedAndNothingReplaced2()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar fubber"));
        buildLogMultilineRegexContent.substText = null;

        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);

        assertEquals("error foo bar fubber\n", result);
    }

    @Test
    public void testGetContent_errorMatchedAndReplacedByString()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar error fubber"));
        buildLogMultilineRegexContent.regex = ".*(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b.*";
        buildLogMultilineRegexContent.substText = "REPLACE";
        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);

        assertEquals("REPLACE\n", result);
    }

    @Test
    public void testGetContent_prefixMatchedTruncatedAndStripped()
            throws Exception {
        when(build.getLogReader()).thenReturn(
                new StringReader("prefix: Yes\nRandom Line\nprefix: No\n"));
        buildLogMultilineRegexContent.regex = "(?:^|(?<=\n))prefix: ((?-s:.*))(?:$|(?=[\r\n]))";
        buildLogMultilineRegexContent.showTruncatedLines = false;
        buildLogMultilineRegexContent.substText = "$1";

        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);

        assertEquals("Yes\nNo\n", result);
    }

    @Test
    public void testGetContent_escapeHtml()
            throws Exception {
        when(build.getLogReader()).thenReturn(
                new StringReader("error <>&\""));
        buildLogMultilineRegexContent.showTruncatedLines = false;
        buildLogMultilineRegexContent.escapeHtml = true;

        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);

        assertEquals("error &lt;&gt;&amp;&quot;\n", result);
    }

    @Test
    public void testGetContent_matchedSegmentHtmlStyleEmpty()
            throws Exception {
        when(build.getLogReader()).thenReturn(
                new StringReader("error"));
        buildLogMultilineRegexContent.showTruncatedLines = false;
        buildLogMultilineRegexContent.matchedSegmentHtmlStyle = "";

        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);

        assertEquals("<pre>\n<b>error</b>\n</pre>\n", result);
    }

    @Test
    public void testGetContent_matchedSegmentHtmlStyle()
            throws Exception {
        when(build.getLogReader()).thenReturn(
                new StringReader("error"));
        buildLogMultilineRegexContent.showTruncatedLines = false;
        buildLogMultilineRegexContent.matchedSegmentHtmlStyle = "color: red";

        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);

        assertEquals("<pre>\n<b style=\"color: red\">error</b>\n</pre>\n", result);
    }

    @Test
    public void testGetContent_shouldStripOutConsoleNotes()
            throws Exception {
        // See HUDSON-7402
        buildLogMultilineRegexContent.regex = ".+";
        buildLogMultilineRegexContent.showTruncatedLines = false;
        when(build.getLogReader()).thenReturn(
                new StringReader(ConsoleNote.PREAMBLE_STR + "AAAAdB+LCAAAAAAAAABb85aBtbiIQSOjNKU4P0+vIKc0PTOvWK8kMze1uCQxtyC1SC8ExvbLL0llgABGJgZGLwaB3MycnMzi4My85FTXgvzkjIoiBimoScn5ecX5Oal6zhAaVS9DRQGQ1uaZsmc5AAaMIAyBAAAA" + ConsoleNote.POSTAMBLE_STR + "No emails were triggered."));

        final String result = buildLogMultilineRegexContent.evaluate(build, listener, BuildLogMultilineRegexContent.MACRO_NAME);

        assertEquals("No emails were triggered.\n", result);
    }
}
