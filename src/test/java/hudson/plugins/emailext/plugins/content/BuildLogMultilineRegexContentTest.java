package hudson.plugins.emailext.plugins.content;

import hudson.console.ConsoleNote;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BuildLogMultilineRegexContentTest
{
  private BuildLogMultilineRegexContent buildLogMultilineRegexContent;

  private Map<String, Object> args;

  @Before
  public void beforeTest()
  {
    buildLogMultilineRegexContent = new BuildLogMultilineRegexContent();

    args = new HashMap<String, Object>();
    args.put("regex", ".+");
  }

  @Test
  public void testGetContent_multilineDotallRegex() throws Exception
  {
    final BufferedReader reader = new BufferedReader( new StringReader(
      "line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
    args.put("regex", "(?s)start:.*end\\.");
    final String result = buildLogMultilineRegexContent.getContent( reader, args );
    assertEquals(
      "[...truncated 2 lines...]\nstart:\r\na\r\nb\r\nc\r\nend.\n[...truncated 3 lines...]\n",
      result);
  }

  @Test
  public void testGetContent_multilineDotallRegex2() throws Exception
  {
    final BufferedReader reader = new BufferedReader( new StringReader(
      "line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
    args.put("regex", "rt:(?s:.*)en");
    final String result = buildLogMultilineRegexContent.getContent( reader, args );
    assertEquals("[...truncated 2 lines...]\nrt:\r\na\r\nb\r\nc\r\nen\n[...truncated 4 lines...]\n",
                 result);
  }

  @Test
  public void testGetContent_multilineEOLRegex() throws Exception
  {
    final BufferedReader reader = new BufferedReader( new StringReader(
      "line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
    args.put("regex", "start:\\r?\\n(.*\\r?\\n)+end\\.");
    final String result = buildLogMultilineRegexContent.getContent( reader, args );
    assertEquals("[...truncated 2 lines...]\nstart:\r\na\r\nb\r\nc\r\nend.\n[...truncated 3 lines...]\n",
                 result);
  }

  @Test
  public void testGetContent_multilineCommentsAlternationsRegex() throws Exception
  {
    final BufferedReader reader = new BufferedReader( new StringReader(
      "line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
    args.put("regex", "(?x)\n"
                      + "# first alternative\n"
                      + "line\\ \\#1(?s:.*)\\#2\n"
                      + "# second alternative\n"
                      + "|start:(?s:.*)end\\."
                      + "# third alternative"
                      + "|xyz(?s:.*)omega");
    final String result = buildLogMultilineRegexContent.getContent( reader, args );
    assertEquals("line #1\r\nline #2\nstart:\r\na\r\nb\r\nc\r\nend.\n[...truncated 3 lines...]\n",
                 result);
  }

  @Test
  public void testGetContent_emptyBuildLogShouldStayEmpty()
    throws Exception
  {
    final BufferedReader reader = new BufferedReader( new StringReader( "" ) );

    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "", result );
  }

  @Test
  public void testGetContent_matchedLines()
    throws Exception
  {
    final BufferedReader reader = new BufferedReader( new StringReader(
      "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n" ) );
    args.put("regex", ".*(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b.*");
    args.put( "showTruncatedLines", false );

    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "6 ERROR\n9 ERROR\n18 ERROR\n", result );
  }

  @Test
  public void testGetContent_truncatedAndMatchedLines()
    throws Exception
  {
    final BufferedReader reader = new BufferedReader(new StringReader(
      "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));

    args.put("regex", ".*(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b.*");
    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "[...truncated 5 lines...]\n6 ERROR\n[...truncated 2 lines...]\n9 ERROR\n[...truncated 8 lines...]\n18 ERROR\n[...truncated 5 lines...]\n", result );
  }

  @Test
  public void testGetContent_errorMatchedAndNothingReplaced()
    throws Exception
  {
    final BufferedReader reader = new BufferedReader( new StringReader( "error foo bar fubber" ) );
    args.put( "substText", "$0");

    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "error foo bar fubber\n", result );
  }

  @Test
  public void testGetContent_errorMatchedAndNothingReplaced2()
    throws Exception
  {
    final BufferedReader reader = new BufferedReader( new StringReader( "error foo bar fubber" ) );
    args.put( "substText", null);

    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "error foo bar fubber\n", result );
  }

  @Test
  public void testGetContent_errorMatchedAndReplacedByString()
    throws Exception
  {
    final BufferedReader reader = new BufferedReader( new StringReader( "error foo bar error fubber" ) );
    args.put("regex", ".*(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b.*");
    args.put( "substText", "REPLACE");
    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "REPLACE\n", result );
  }

  @Test
  public void testGetContent_prefixMatchedTruncatedAndStripped()
    throws Exception
  {
    final BufferedReader reader = new BufferedReader(
      new StringReader( "prefix: Yes\nRandom Line\nprefix: No\n" ) );
    args.put( "regex", "(?:^|(?<=\n))prefix: ((?-s:.*))(?:$|(?=[\r\n]))");
    args.put( "showTruncatedLines", false);
    args.put( "substText", "$1");

    final String result = buildLogMultilineRegexContent.getContent( reader, args );

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

    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "error &lt;&gt;&amp;&quot;\n", result );
  }

  @Test
  public void testGetContent_matchedSegmentHtmlStyleEmpty()
    throws Exception
  {
    final BufferedReader reader = new BufferedReader(
      new StringReader( "error" ) );
    args.put( "showTruncatedLines", false );
    args.put( "matchedSegmentHtmlStyle", "" );

    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "<pre>\n<b>error</b>\n</pre>\n", result );
  }

  @Test
  public void testGetContent_matchedSegmentHtmlStyle()
    throws Exception
  {
    final BufferedReader reader = new BufferedReader(
      new StringReader( "error" ) );
    args.put( "showTruncatedLines", false );
    args.put( "matchedSegmentHtmlStyle", "color: red");

    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "<pre>\n<b style=\"color: red\">error</b>\n</pre>\n", result );
  }

  @Test
  public void testGetContent_shouldStripOutConsoleNotes()
    throws Exception
  {
    // See HUDSON-7402
    args.put( "regex", ".+");
    args.put( "showTruncatedLines", false);
    final BufferedReader reader = new BufferedReader(
      new StringReader( ConsoleNote.PREAMBLE_STR + "AAAAdB+LCAAAAAAAAABb85aBtbiIQSOjNKU4P0+vIKc0PTOvWK8kMze1uCQxtyC1SC8ExvbLL0llgABGJgZGLwaB3MycnMzi4My85FTXgvzkjIoiBimoScn5ecX5Oal6zhAaVS9DRQGQ1uaZsmc5AAaMIAyBAAAA" + ConsoleNote.POSTAMBLE_STR + "No emails were triggered." ) );

    final String result = buildLogMultilineRegexContent.getContent( reader, args );

    assertEquals( "No emails were triggered.\n", result);
  }
}
