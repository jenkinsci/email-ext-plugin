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

public class BuildLogRegexContentTest {

    private BuildLogRegexContent buildLogRegexContent;
    private TaskListener listener;
    private AbstractBuild build;

    @Before
    public void beforeTest() {
        buildLogRegexContent = new BuildLogRegexContent();
        listener = new StreamTaskListener(System.out);
        build = mock(AbstractBuild.class);
    }

    @Test
    public void testGetContent_emptyBuildLogShouldStayEmpty()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(""));

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("", result);
    }

    @Test
    public void testGetContent_matchedLines()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        buildLogRegexContent.showTruncatedLines = false;

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("6 ERROR\n9 ERROR\n18 ERROR\n", result);
    }

    @Test
    public void testGetContent_truncatedAndMatchedLines()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("[...truncated 5 lines...]\n6 ERROR\n[...truncated 2 lines...]\n9 ERROR\n[...truncated 8 lines...]\n18 ERROR\n[...truncated 5 lines...]\n", result);
    }

    @Test
    public void testGetContent_truncatedMatchedAndContextLines()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        buildLogRegexContent.linesBefore = 3;
        buildLogRegexContent.linesAfter = 3;
        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("[...truncated 2 lines...]\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n[...truncated 2 lines...]\n15\n16\n17\n18 ERROR\n19\n20\n21\n[...truncated 2 lines...]\n", result);
    }

    @Test
    public void testGetContent_matchedAndContextLines()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        buildLogRegexContent.showTruncatedLines = false;
        buildLogRegexContent.linesBefore = 3;
        buildLogRegexContent.linesAfter = 3;
        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n15\n16\n17\n18 ERROR\n19\n20\n21\n", result);
    }

    @Test
    public void testGetContent_truncatedMatchedAndContextLinesAsHtml()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        buildLogRegexContent.matchedLineHtmlStyle = "color: red";
        buildLogRegexContent.linesBefore = 3;
        buildLogRegexContent.linesAfter = 3;
        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("<p>[...truncated 2 lines...]</p>\n<pre>\n3\n4\n5\n<b style=\"color: red\">6 ERROR</b>\n7\n8\n<b style=\"color: red\">9 ERROR</b>\n10\n11\n12\n</pre>\n<p>[...truncated 2 lines...]</p>\n<pre>\n15\n16\n17\n<b style=\"color: red\">18 ERROR</b>\n19\n20\n21\n</pre>\n<p>[...truncated 2 lines...]</p>\n", result);
    }

    @Test
    public void testGetContent_matchedAndContextLinesAsHtml()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(
                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        buildLogRegexContent.matchedLineHtmlStyle = "color: red";
        buildLogRegexContent.linesBefore = 3;
        buildLogRegexContent.linesAfter = 3;
        buildLogRegexContent.showTruncatedLines = false;
        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("<pre>\n3\n4\n5\n<b style=\"color: red\">6 ERROR</b>\n7\n8\n<b style=\"color: red\">9 ERROR</b>\n10\n11\n12\n15\n16\n17\n<b style=\"color: red\">18 ERROR</b>\n19\n20\n21\n</pre>\n", result);
    }

    @Test
    public void testGetContent_errorMatchedAndNothingReplaced()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar fubber"));
        buildLogRegexContent.substText = "$0";

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("error foo bar fubber\n", result);
    }

    @Test
    public void testGetContent_errorMatchedAndNothingReplaced2()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar fubber"));
        buildLogRegexContent.substText = null;

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("error foo bar fubber\n", result);
    }

    @Test
    public void testGetContent_errorMatchedAndReplacedByString()
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar error fubber"));
        buildLogRegexContent.substText = "REPLACE";

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("REPLACE foo bar REPLACE fubber\n", result);
    }

    @Test
    public void testGetContent_prefixMatchedTruncatedAndStripped()
            throws Exception {
        when(build.getLogReader()).thenReturn(
                new StringReader("prefix: Yes\nRandom Line\nprefix: No\n"));
        buildLogRegexContent.regex = "^prefix: (.*)$";
        buildLogRegexContent.showTruncatedLines = false;
        buildLogRegexContent.substText = "$1";

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("Yes\nNo\n", result);
    }

    @Test
    public void testGetContent_escapeHtml()
            throws Exception {
        when(build.getLogReader()).thenReturn(
                new StringReader("error <>&\""));
        buildLogRegexContent.showTruncatedLines = false;
        buildLogRegexContent.escapeHtml = true;

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("error &lt;&gt;&amp;&quot;\n", result);
    }

    @Test
    public void testGetContent_matchedLineHtmlStyleEmpty()
            throws Exception {
        when(build.getLogReader()).thenReturn(
                new StringReader("error"));
        buildLogRegexContent.showTruncatedLines = false;
        buildLogRegexContent.matchedLineHtmlStyle = "";

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("<pre>\n<b>error</b>\n</pre>\n", result);
    }

    @Test
    public void testGetContent_matchedLineHtmlStyle()
            throws Exception {
        when(build.getLogReader()).thenReturn(
                new StringReader("error"));
        buildLogRegexContent.showTruncatedLines = false;
        buildLogRegexContent.matchedLineHtmlStyle = "color: red";

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("<pre>\n<b style=\"color: red\">error</b>\n</pre>\n", result);
    }

    @Test
    public void testGetContent_shouldStripOutConsoleNotes()
            throws Exception {
        // See HUDSON-7402
        buildLogRegexContent.regex = ".*";
        buildLogRegexContent.showTruncatedLines = false;
        when(build.getLogReader()).thenReturn(
                new StringReader(ConsoleNote.PREAMBLE_STR + "AAAAdB+LCAAAAAAAAABb85aBtbiIQSOjNKU4P0+vIKc0PTOvWK8kMze1uCQxtyC1SC8ExvbLL0llgABGJgZGLwaB3MycnMzi4My85FTXgvzkjIoiBimoScn5ecX5Oal6zhAaVS9DRQGQ1uaZsmc5AAaMIAyBAAAA" + ConsoleNote.POSTAMBLE_STR + "No emails were triggered."));

        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("No emails were triggered.\n", result);
    }

    @Test
    public void testGetContent_addNewLineFalse()
            throws Exception {
        // See JENKINS-14320
        buildLogRegexContent.addNewline = false;
        buildLogRegexContent.regex = "^\\*{3} Application: (.*)$";
        buildLogRegexContent.maxMatches = 1;
        buildLogRegexContent.showTruncatedLines = false;
        buildLogRegexContent.substText = "$1";
        when(build.getLogReader()).thenReturn(
                new StringReader("*** Application: Firefox 15.0a2\n*** Platform: Mac OS X 10.7.4 64bit"));
        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("Firefox 15.0a2", result);
    }

    @Test
    public void testGetContent_defaultValue()
            throws Exception {
        // See JENKINS-16269
        buildLogRegexContent.defaultValue = "JENKINS";
        buildLogRegexContent.regex = "^\\*{3} Blah Blah: (.*)$";
        buildLogRegexContent.maxMatches = 1;
        buildLogRegexContent.showTruncatedLines = false;
        buildLogRegexContent.substText = "$1";
        when(build.getLogReader()).thenReturn(
                new StringReader("*** Application: Firefox 15.0a2\n*** Platform: Mac OS X 10.7.4 64bit"));
        final String result = buildLogRegexContent.evaluate(build, listener, BuildLogRegexContent.MACRO_NAME);

        assertEquals("JENKINS", result);
    }
}
