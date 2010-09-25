package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
public class ChangesSinceLastUnstableBuildContentTest {

    private ChangesSinceLastUnstableBuildContent content;

    private Map<String, Object> args;

    @Before
    public void setUp() {
        content = new ChangesSinceLastUnstableBuildContent();

        args = new HashMap<String, Object>();
    }

    @Test
    public void testGetContent_shouldGetNoContentSinceUnstableBuildIfNoPreviousBuild() {
        AbstractBuild build = mock(AbstractBuild.class);

        String contentStr = content.getContent(build, null, null, args);

        assertEquals("", contentStr);
    }

    @Test
    public void testGetContent_shouldGetPreviousBuildFailures() {
        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.getContent(currentBuild, null, null, args);

        assertEquals("Changes for Build #41\n" +
                "[Ash Lux] Changes for a failed build.\n" +
                "\n" +
                "\n" +
                "Changes for Build #42\n" +
                "[Ash Lux] Changes for a successful build.\n" +
                "\n" +
                "\n", contentStr);
    }

    @Test
    public void testGetContent_whenReverseOrderIsTrueShouldReverseOrderOfChanges() {
        args.put( AbstractChangesSinceContent.REVERSE_ARG_NAME, true );

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.getContent(currentBuild, null, null, args);

        assertEquals("Changes for Build #42\n" + "[Ash Lux] Changes for a successful build.\n" + "\n" + "\n" +
            "Changes for Build #41\n" + "[Ash Lux] Changes for a failed build.\n" + "\n" + "\n", contentStr);
    }
    
    @Test
    public void testGetContent_shouldGetPreviousBuildsThatArentUnstable_HUDSON3519() {
        // Test for HUDSON-3519

        AbstractBuild successfulBuild = createBuild(Result.SUCCESS, 2, "Changes for a successful build.");

        AbstractBuild unstableBuild = createBuild(Result.UNSTABLE, 3, "Changes for an unstable build.");
        when (unstableBuild.getPreviousBuild()).thenReturn(successfulBuild);
        when (successfulBuild.getNextBuild()).thenReturn(unstableBuild);

        AbstractBuild abortedBuild = createBuild(Result.ABORTED, 4, "Changes for an aborted build.");
        when (abortedBuild.getPreviousBuild()).thenReturn(unstableBuild);
        when (unstableBuild.getNextBuild()).thenReturn(abortedBuild);

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 5, "Changes for a failed build.");
        when (failureBuild.getPreviousBuild()).thenReturn(abortedBuild);
        when(abortedBuild.getNextBuild()).thenReturn(failureBuild);

        AbstractBuild notBuiltBuild = createBuild(Result.NOT_BUILT, 6, "Changes for a not-built build.");
        when (notBuiltBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(notBuiltBuild);

        AbstractBuild currentBuild = createBuild(Result.UNSTABLE, 7, "Changes for an unstable build.");
        when(currentBuild.getPreviousBuild()).thenReturn(notBuiltBuild);
        when(notBuiltBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.getContent(currentBuild, null, null, args);

        assertEquals(
                "Changes for Build #4\n" +
                "[Ash Lux] Changes for an aborted build.\n" +
                "\n" +
                "\n" +
                "Changes for Build #5\n" +
                "[Ash Lux] Changes for a failed build.\n" +
                "\n" +
                "\n" +
                "Changes for Build #6\n" +
                "[Ash Lux] Changes for a not-built build.\n" +
                "\n" +
                "\n" +
                "Changes for Build #7\n" +
                "[Ash Lux] Changes for an unstable build.\n" +
                "\n" +
                "\n", contentStr);
    }
    
    @Test
    public void testShouldPrintDate()
    {
        args.put( ChangesSinceLastSuccessfulBuildContent.CHANGES_FORMAT_ARG_NAME, "%d" );

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.getContent( currentBuild, null, null, args );

        Assert.assertEquals( "Changes for Build #41\n" + "DATE\n" + "Changes for Build #42\n" + "DATE\n", contentStr );
    }

    @Test
    public void testShouldPrintRevision()
    {
        args.put( ChangesSinceLastSuccessfulBuildContent.CHANGES_FORMAT_ARG_NAME, "%r" );

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.getContent( currentBuild, null, null, args );

        Assert.assertEquals( "Changes for Build #41\n" + "REVISION\n" + "Changes for Build #42\n" + "REVISION\n", contentStr );
    }

    @Test
    public void testShouldPrintPath()
    {
        args.put( ChangesSinceLastSuccessfulBuildContent.CHANGES_FORMAT_ARG_NAME, "%p" );

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.getContent( currentBuild, null, null, args );

        Assert.assertEquals( "Changes for Build #41\n" + "\tPATH1\n" + "\tPATH2\n" + "\tPATH3\n" + "\n" +
            "Changes for Build #42\n" + "\tPATH1\n" + "\tPATH2\n" + "\tPATH3\n" + "\n", contentStr );
    }

    @Test
    public void testWhenShowPathsIsTrueShouldPrintPath()
    {
        args.put( ChangesSinceLastBuildContent.SHOW_PATHS_ARG_NAME, true );

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.getContent( currentBuild, null, null, args );

        Assert.assertEquals( "Changes for Build #41\n" + "[Ash Lux] Changes for a failed build.\n" + "\tPATH1\n" +
            "\tPATH2\n" + "\tPATH3\n" + "\n" + "\n" + "Changes for Build #42\n" +
            "[Ash Lux] Changes for a successful build.\n" + "\tPATH1\n" + "\tPATH2\n" + "\tPATH3\n" + "\n" + "\n", contentStr );
    }
    
    private AbstractBuild createBuild(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getResult()).thenReturn(result);
        ChangeLogSet changes1 = createChangeLog(message);
        when(build.getChangeSet()).thenReturn(changes1);
        when(build.getNumber()).thenReturn(buildNumber);

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
