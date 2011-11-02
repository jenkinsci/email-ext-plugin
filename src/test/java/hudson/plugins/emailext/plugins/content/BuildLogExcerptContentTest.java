package hudson.plugins.emailext.plugins.content;

import static org.junit.Assert.*;

import hudson.console.ConsoleNote;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class BuildLogExcerptContentTest
{
    private BuildLogExcerptContent buildLogExcerptContent;

    private Map<String, Object> args;

    @Before
    public void beforeTest()
    {
        buildLogExcerptContent = new BuildLogExcerptContent();

        args = new HashMap<String, Object>();
    }

    @Test
    public void testGetContent_emptyBuildLogShouldStayEmpty()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader( new StringReader( "" ) );
        args.put( "start", "START" );
        args.put( "end", "END" );

        final String result = buildLogExcerptContent.getContent( reader, args );

        assertEquals( "", result );
    }

	@Test
    public void testGetContent_simpleStartEndTags()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader( new StringReader(
			"1\n2\n3\n4\n5\nSTART\n7\n8\n9\nEND\n10\n11\n12\n" ) );
		args.put( "start", "START" );
        args.put( "end", "END" );

        final String result = buildLogExcerptContent.getContent( reader, args );

        assertEquals( "7\n8\n9\n", result );
    }

	@Test
    public void testGetContent_regexpStartEndTags()
        throws Exception
    {
        final BufferedReader reader = new BufferedReader( new StringReader(
			"1\n2\n3\n4\n5\nTEST STARTED\n7\n8\n9\nTEST STOPED\n10\n11\n12\n" ) );
		args.put( "start", ".*START.*" );
        args.put( "end", ".*STOP.*" );

        final String result = buildLogExcerptContent.getContent( reader, args );

        assertEquals( "7\n8\n9\n", result );
    }


}
