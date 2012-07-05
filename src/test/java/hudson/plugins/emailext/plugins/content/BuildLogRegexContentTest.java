package hudson.plugins.emailext.plugins.content;

import hudson.console.ConsoleNote;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BuildLogRegexContentTest
{
    private BuildLogRegexContent buildLogRegexContent;

    private Map<String, Object> args;

    @Before
    public void beforeTest()
    {
        buildLogRegexContent = new BuildLogRegexContent();

        args = new HashMap<String, Object>();
    }

    @Test
    public void testGetContent_emptyBuildLogShouldStayEmpty()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader( new StringReader( "" ) );

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "", result );
    }

    @Test
    public void testGetContent_matchedLines()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader( new StringReader(
            "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n" ) );
        args.put( "showTruncatedLines", false );

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "6 ERROR\n9 ERROR\n18 ERROR\n", result );
    }

    @Test
    public void testGetContent_truncatedAndMatchedLines()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader(new StringReader(
            "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "[...truncated 5 lines...]\n6 ERROR\n[...truncated 2 lines...]\n9 ERROR\n[...truncated 8 lines...]\n18 ERROR\n[...truncated 5 lines...]\n", result );
    }

    @Test
    public void testGetContent_truncatedMatchedAndContextLines()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader(new StringReader(
            "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        args.put( "linesBefore", 3 );
        args.put( "linesAfter", 3 );
        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "[...truncated 2 lines...]\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n[...truncated 2 lines...]\n15\n16\n17\n18 ERROR\n19\n20\n21\n[...truncated 2 lines...]\n", result );
    }

    @Test
    public void testGetContent_matchedAndContextLines()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader(new StringReader(
            "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        args.put( "showTruncatedLines", false );
        args.put( "linesBefore", 3 );
        args.put( "linesAfter", 3 );
        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n15\n16\n17\n18 ERROR\n19\n20\n21\n", result );
    }

    @Test
    public void testGetContent_truncatedMatchedAndContextLinesAsHtml()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader(new StringReader(
            "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        args.put( "matchedLineHtmlStyle", "color: red" );
        args.put( "linesBefore", 3 );
        args.put( "linesAfter", 3 );
        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "<p>[...truncated 2 lines...]</p>\n<pre>\n3\n4\n5\n<b style=\"color: red\">6 ERROR</b>\n7\n8\n<b style=\"color: red\">9 ERROR</b>\n10\n11\n12\n</pre>\n<p>[...truncated 2 lines...]</p>\n<pre>\n15\n16\n17\n<b style=\"color: red\">18 ERROR</b>\n19\n20\n21\n</pre>\n<p>[...truncated 2 lines...]</p>\n", result );
    }

    @Test
    public void testGetContent_matchedAndContextLinesAsHtml()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader(new StringReader(
            "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        args.put( "matchedLineHtmlStyle", "color: red" );
        args.put( "linesBefore", 3 );
        args.put( "linesAfter", 3 );
        args.put( "showTruncatedLines", false );
        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "<pre>\n3\n4\n5\n<b style=\"color: red\">6 ERROR</b>\n7\n8\n<b style=\"color: red\">9 ERROR</b>\n10\n11\n12\n15\n16\n17\n<b style=\"color: red\">18 ERROR</b>\n19\n20\n21\n</pre>\n", result );
    }

    @Test
    public void testGetContent_errorMatchedAndNothingReplaced()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader( new StringReader( "error foo bar fubber" ) );
        args.put( "substText", "$0");

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "error foo bar fubber\n", result );
    }

    @Test
    public void testGetContent_errorMatchedAndNothingReplaced2()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader( new StringReader( "error foo bar fubber" ) );
        args.put( "substText", null);

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "error foo bar fubber\n", result );
    }

    @Test
    public void testGetContent_errorMatchedAndReplacedByString()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader( new StringReader( "error foo bar error fubber" ) );
        args.put( "substText", "REPLACE");

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "REPLACE foo bar REPLACE fubber\n", result );
    }
    
    @Test
    public void testGetContent_prefixMatchedTruncatedAndStripped()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader( 
                new StringReader( "prefix: Yes\nRandom Line\nprefix: No\n" ) );
        args.put( "regex", "^prefix: (.*)$");
        args.put( "showTruncatedLines", false);
        args.put( "substText", "$1");

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "Yes\nNo\n", result );
    }

    @Test
    public void testGetContent_escapeHtml()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader(
                new StringReader( "error <>&\"" ) );
        args.put( "showTruncatedLines", false );
        args.put( "escapeHtml", true );

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "error &lt;&gt;&amp;&quot;\n", result );
    }

    @Test
    public void testGetContent_matchedLineHtmlStyleEmpty()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader(
                new StringReader( "error" ) );
        args.put( "showTruncatedLines", false );
        args.put( "matchedLineHtmlStyle", "" );

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "<pre>\n<b>error</b>\n</pre>\n", result );
    }

    @Test
    public void testGetContent_matchedLineHtmlStyle()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader(
                new StringReader( "error" ) );
        args.put( "showTruncatedLines", false );
        args.put( "matchedLineHtmlStyle", "color: red");

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "<pre>\n<b style=\"color: red\">error</b>\n</pre>\n", result );
    }

    @Test
    public void testGetContent_shouldStripOutConsoleNotes()
            throws Exception
    {
        // See HUDSON-7402
        args.put( "regex", ".*");
        args.put( "showTruncatedLines", false);
        final BufferedReader reader = new BufferedReader(
                new StringReader( ConsoleNote.PREAMBLE_STR + "AAAAdB+LCAAAAAAAAABb85aBtbiIQSOjNKU4P0+vIKc0PTOvWK8kMze1uCQxtyC1SC8ExvbLL0llgABGJgZGLwaB3MycnMzi4My85FTXgvzkjIoiBimoScn5ecX5Oal6zhAaVS9DRQGQ1uaZsmc5AAaMIAyBAAAA" + ConsoleNote.POSTAMBLE_STR + "No emails were triggered." ) );

        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "No emails were triggered.\n", result);
    }

    @Test
    public void testGetContent_addNewLineFalse()
            throws Exception
    {
        // See JENKINS-14320
        args.put( "addNewline", false );
        args.put( "regex", "^\\*{3} Application: (.*)$" );
        args.put( "maxMatches", 1);
        args.put( "showTruncatedLines", false );
        args.put( "substText", "$1" );
        final BufferedReader reader = new BufferedReader(
                new StringReader( "*** Application: Firefox 15.0a2\n*** Platform: Mac OS X 10.7.4 64bit" ));
        final String result = buildLogRegexContent.getContent( reader, args );

        assertEquals( "Firefox 15.0a2", result);
    }
}
