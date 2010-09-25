package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings( { "unchecked" } )
public class ChangesSinceLastBuildContentTest
{
    private ChangesSinceLastBuildContent changesSinceLastBuildContent;

    private Map<String, Object> args;

    @Before
    public void setup()
    {
        changesSinceLastBuildContent = new ChangesSinceLastBuildContent();

        args = new HashMap<String, Object>();
    }

    @Test
    public void testShouldGetChangesForLatestBuild()
    {
        AbstractBuild currentBuild = createBuild( Result.SUCCESS, 42, "Changes for a successful build." );

        String content = changesSinceLastBuildContent.getContent( currentBuild, null, null, args );

        assertEquals( "[Ash Lux] Changes for a successful build.\n\n", content );
    }

    @Test
    public void testShouldGetChangesForLatestBuildEvenWhenPreviousBuildsExist()
    {
        AbstractBuild failureBuild = createBuild( Result.FAILURE, 41, "Changes for a failed build." );

        AbstractBuild currentBuild = createBuild( Result.SUCCESS, 42, "Changes for a successful build." );
        when( currentBuild.getPreviousBuild() ).thenReturn( failureBuild );
        when( failureBuild.getNextBuild() ).thenReturn( currentBuild );

        String content = changesSinceLastBuildContent.getContent( currentBuild, null, null, args );

        assertEquals( "[Ash Lux] Changes for a successful build.\n\n", content );
    }

    @Test
    public void testShouldPrintDate()
    {
        args.put( ChangesSinceLastBuildContent.FORMAT_ARG_NAME, "%d" );

        AbstractBuild currentBuild = createBuild( Result.SUCCESS, 42, "Changes for a successful build." );

        String content = changesSinceLastBuildContent.getContent( currentBuild, null, null, args );

        assertEquals( "DATE", content );
    }

    @Test
    public void testShouldPrintRevision()
    {
        args.put( ChangesSinceLastBuildContent.FORMAT_ARG_NAME, "%r" );

        AbstractBuild currentBuild = createBuild( Result.SUCCESS, 42, "Changes for a successful build." );

        String content = changesSinceLastBuildContent.getContent( currentBuild, null, null, args );

        assertEquals( "REVISION", content );
    }

    @Test
    public void testShouldPrintPath()
    {
        args.put( ChangesSinceLastBuildContent.FORMAT_ARG_NAME, "%p" );

        AbstractBuild currentBuild = createBuild( Result.SUCCESS, 42, "Changes for a successful build." );

        String content = changesSinceLastBuildContent.getContent( currentBuild, null, null, args );

        assertEquals( "\tPATH1\n\tPATH2\n\tPATH3\n", content );
    }

    @Test
    public void testWhenShowPathsIsTrueShouldPrintPath()
    {
        args.put( ChangesSinceLastBuildContent.SHOW_PATHS_ARG_NAME, true );

        AbstractBuild currentBuild = createBuild( Result.SUCCESS, 42, "Changes for a successful build." );

        String content = changesSinceLastBuildContent.getContent( currentBuild, null, null, args );

        assertEquals( "[Ash Lux] Changes for a successful build.\n" + "\tPATH1\n" + "\tPATH2\n" + "\tPATH3\n" + "\n", content );
    }

    private AbstractBuild createBuild( Result result, int buildNumber, String message )
    {
        AbstractBuild build = mock( AbstractBuild.class );
        when( build.getResult() ).thenReturn( result );
        ChangeLogSet changes1 = createChangeLog( message );
        when( build.getChangeSet() ).thenReturn( changes1 );
        when( build.getNumber() ).thenReturn( buildNumber );

        return build;
    }

    public ChangeLogSet createChangeLog( String message )
    {
        ChangeLogSet changes = mock( ChangeLogSet.class );

        List<ChangeLogSet.Entry> entries = new LinkedList<ChangeLogSet.Entry>();
        ChangeLogSet.Entry entry = new ChangeLogEntry( message, "Ash Lux" );
        entries.add( entry );
        when( changes.iterator() ).thenReturn( entries.iterator() );

        return changes;
    }

    public class ChangeLogEntry
        extends ChangeLogSet.Entry
    {
        final String message;

        final String author;

        public ChangeLogEntry( String message, String author )
        {
            this.message = message;
            this.author = author;
        }

        @Override
        public String getMsg()
        {
            return message;
        }

        @Override
        public User getAuthor()
        {
            User user = mock( User.class );
            when( user.getFullName() ).thenReturn( author );
            return user;
        }

        @Override
        public Collection<String> getAffectedPaths()
        {
            return new ArrayList<String>()
            {{
                    add( "PATH1" );
                    add( "PATH2" );
                    add( "PATH3" );
                }};
        }

        @SuppressWarnings( { "UnusedDeclaration" } )
        public String getRevision()
        {
            return "REVISION";
        }

        @SuppressWarnings( { "UnusedDeclaration" } )
        public String getDate()
        {
            return "DATE";
        }
    }
}
