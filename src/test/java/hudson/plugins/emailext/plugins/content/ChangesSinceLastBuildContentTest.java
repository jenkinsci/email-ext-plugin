package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.Messages;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.EditType;
import hudson.util.StreamTaskListener;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        listener = StreamTaskListener.fromStdout();
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
    
    @Test
    public void testTypeFormatStringWithNoGetAffectedFiles()
        throws Exception {
        changesSinceLastBuildContent.showPaths = true;
        changesSinceLastBuildContent.pathFormat = "\t%p\t%a\n";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("[Ash Lux] Changes for a successful build.\n" + "\tPATH1\tUnknown\n" + "\tPATH2\tUnknown\n" + "\tPATH3\tUnknown\n" + "\n", content);
    }
    
    @Test
    public void testTypeFormatStringWithAffectedFiles()
        throws Exception {
        changesSinceLastBuildContent.showPaths = true;
        changesSinceLastBuildContent.pathFormat = "\t%p\t%a - %d\n";

        AbstractBuild currentBuild = createBuildWithAffectedFiles(Result.SUCCESS, 42, "Changes for a successful build.");

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("[Ash Lux] Changes for a successful build.\n" + "\tPATH1\tadd - The file was added\n" + "\tPATH2\tdelete - The file was removed\n" + "\tPATH3\tedit - The file was modified\n" + "\n", content);
    }

    @Test
    public void testRegexReplace()
            throws Exception {
        changesSinceLastBuildContent.regex = "<defectId>(DEFECT-[0-9]+)</defectId><message>(.*)</message>";
        changesSinceLastBuildContent.replace = "[$1] $2";
        changesSinceLastBuildContent.format = "%m\\n";

        AbstractBuild currentBuild = createBuildWithAffectedFiles(Result.SUCCESS, 42, "<defectId>DEFECT-666</defectId><message>Initial commit</message>");

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("[DEFECT-666] Initial commit\n\n", content);
    }

    @Test
    public void testShouldPrintDefaultMessageWhenNoChanges()
            throws Exception {
        AbstractBuild currentBuild = createBuildWithNoChanges(Result.SUCCESS, 42);

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals(ChangesSinceLastBuildContent.DEFAULT_DEFAULT_VALUE, content);
    }

    @Test
    public void testShouldPrintMessageWhenNoChanges()
            throws Exception {
        changesSinceLastBuildContent.def = "another default message\n";
        AbstractBuild currentBuild = createBuildWithNoChanges(Result.SUCCESS, 42);

        String content = changesSinceLastBuildContent.evaluate(currentBuild, listener, ChangesSinceLastBuildContent.MACRO_NAME);

        assertEquals("another default message\n", content);
    }

    private AbstractBuild createBuild(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getResult()).thenReturn(result);
        ChangeLogSet changes1 = createChangeLog(message);
        when(build.getChangeSet()).thenReturn(changes1);
        when(build.getNumber()).thenReturn(buildNumber);

        return build;
    }
    
    private AbstractBuild createBuildWithAffectedFiles(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getResult()).thenReturn(result);
        ChangeLogSet changes1 = createChangeLogWithAffectedFiles(message);
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
    
    private AbstractBuild createBuildWithNoChanges(Result result, int buildNumber) {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getResult()).thenReturn(result);
        ChangeLogSet changes1 = createEmptyChangeLog();
        when(build.getChangeSet()).thenReturn(changes1);
        when(build.getNumber()).thenReturn(buildNumber);

        return build;
    }

    public ChangeLogSet createEmptyChangeLog() {
        ChangeLogSet changes = mock(ChangeLogSet.class);
        List<ChangeLogSet.Entry> entries = Collections.emptyList();
        when(changes.iterator()).thenReturn(entries.iterator());
        when(changes.isEmptySet()).thenReturn(true);

        return changes;
    }
    
    public ChangeLogSet createChangeLogWithAffectedFiles(String message) {
        ChangeLogSet changes = mock(ChangeLogSet.class);

        List<ChangeLogSet.Entry> entries = new LinkedList<ChangeLogSet.Entry>();
        ChangeLogSet.Entry entry = new ChangeLogEntryWithAffectedFiles(message, "Ash Lux");
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
    
    public static class TestAffectedFile implements AffectedFile {
        
        private final String path;
        private final EditType type;
        
        public TestAffectedFile(String path, EditType type) {
            this.path = path;
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public EditType getEditType() {
            return type;
        }        
    }
    
    public static class ChangeLogEntryWithAffectedFiles
            extends ChangeLogEntry {

        public ChangeLogEntryWithAffectedFiles(String message, String author) {
            super(message, author);
        }
        
        @Override
        public Collection<AffectedFile> getAffectedFiles() {
            return new ArrayList<AffectedFile>() {
                {
                    add(new TestAffectedFile("PATH1", EditType.ADD));
                    add(new TestAffectedFile("PATH2", EditType.DELETE));
                    add(new TestAffectedFile("PATH3", EditType.EDIT));
                }
            };
        }                
    }
}
