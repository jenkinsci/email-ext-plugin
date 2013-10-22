package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.util.StreamTaskListener;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
public class ChangesSinceLastBuildContentTest {

    private ChangesSinceLastBuildContent changesSinceLastBuildContent;
    private TaskListener listener;

    @Before
    public void setup() {
        changesSinceLastBuildContent = new ChangesSinceLastBuildContent();
        listener = new StreamTaskListener(System.out);
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Phoenix"));
    }

    @Test
    public void testShouldGetChangesForLatestBuild()
            throws Exception {
        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("[Ash Lux] Changes for a successful build.\n\n", content);
    }

    @Test
    public void testShouldGetChangesForLatestBuildEvenWhenPreviousBuildsExist()
            throws Exception {
        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("[Ash Lux] Changes for a successful build.\n\n", content);
    }

    @Test
    public void testShouldPrintDate()
            throws Exception {
        changesSinceLastBuildContent.format = "%d";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("Oct 21, 2013 7:39:00 PM", content);
    }

    @Test
    public void testShouldPrintRevision()
            throws Exception {
        changesSinceLastBuildContent.format = "%r";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("REVISION", content);
    }

    @Test
    public void testShouldPrintPath()
            throws Exception {
        changesSinceLastBuildContent.format = "%p";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("\tPATH1\n\tPATH2\n\tPATH3\n", content);
    }

    @Test
    public void testWhenShowPathsIsTrueShouldPrintPath()
            throws Exception {
        changesSinceLastBuildContent.showPaths = true;

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("[Ash Lux] Changes for a successful build.\n" + "\tPATH1\n" + "\tPATH2\n" + "\tPATH3\n" + "\n", content);
    }

    @Test
    public void testDateFormatString()
        throws Exception {
        changesSinceLastBuildContent.format = "%d";
        changesSinceLastBuildContent.dateFormat = "MMM d, yyyy HH:mm:ss";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("Oct 21, 2013 19:39:00", content);
    }

    private AbstractBuild createBuild(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getResult()).thenReturn(result);
        ChangeLogSet changes1 = createChangeLog(message);
        when(build.getChangeSet()).thenReturn(changes1);
        when(build.getNumber()).thenReturn(buildNumber);

        return build;
    }

    public ChangeLogSet createChangeLog(String message) {
        ChangeLogSet changes = mock(ChangeLogSet.class);

        List<ChangeLogSet.Entry> entries = new LinkedList<ChangeLogSet.Entry>();
        ChangeLogSet.Entry entry = new ChangeLogEntry(message, "Ash Lux");
        entries.add(entry);
        when(changes.iterator()).thenReturn(entries.iterator());

        return changes;
    }

    public static class ChangeLogEntry
            extends ChangeLogSet.Entry {

        final String message;
        final String author;

        public ChangeLogEntry(String message, String author) {
            this.message = message;
            this.author = author;
        }

        @Override
        public String getMsg() {
            return message;
        }

        @Override
        public User getAuthor() {
            User user = mock(User.class);
            when(user.getFullName()).thenReturn(author);
            return user;
        }

        @Override
        public Collection<String> getAffectedPaths() {
            return new ArrayList<String>() {
                {
                    add("PATH1");
                    add("PATH2");
                    add("PATH3");
                }
            };
        }

        @Override
        public String getCommitId() {
            return "REVISION";
        }

        @Override
        public long getTimestamp() {
            // 10/21/13 7:39 PM
            return 1382409540000L;
        }
    }
}
