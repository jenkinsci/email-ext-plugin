package hudson.plugins.emailext.plugins.content;

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
}
