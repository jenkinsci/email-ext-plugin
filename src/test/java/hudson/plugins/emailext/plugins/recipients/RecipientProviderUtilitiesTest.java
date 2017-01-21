package hudson.plugins.emailext.plugins.recipients;

import hudson.model.Run;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        WorkflowRun.class
})
public class RecipientProviderUtilitiesTest {
    public class Debug implements RecipientProviderUtilities.IDebug {

        @Override
        public void send(String format, Object... args) {

        }
    }

    public static Set<String> usersToEMails(Set<User> users) {
        Set<String> emails = new HashSet<>(users.size());
        for (User user : users) {
            emails.add(user.getProperty(Mailer.UserProperty.class).getAddress());
        }
        return emails;
    }

    @Test
    public void getChangeSetAuthors() throws Exception {
        Debug debug = new Debug();
        WorkflowRun run1 = mock(WorkflowRun.class);
        Set<User> authors = RecipientProviderUtilities.getChangeSetAuthors(Collections.<Run<?, ?>>singleton(run1), debug);
        assertThat(authors, IsCollectionWithSize.hasSize(0));

        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet1 = MockUtilities.makeChangeSet(run1, "A");
        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet2 = MockUtilities.makeChangeSet(run1, "B");
        when(run1.getChangeSets()).thenReturn(Arrays.asList(changeSet1, changeSet2));

        authors = RecipientProviderUtilities.getChangeSetAuthors(Collections.<Run<?, ?>>singleton(run1), debug);
        assertThat(usersToEMails(authors), CoreMatchers.<Set<String>>equalTo(new HashSet<>(Arrays.asList("A@DOMAIN", "B@DOMAIN"))));

        WorkflowRun run2 = mock(WorkflowRun.class);
        MockUtilities.addChangeSet(run2, "C");

        authors = RecipientProviderUtilities.getChangeSetAuthors(Arrays.<Run<?, ?>>asList(run1, run2), debug);
        assertThat(usersToEMails(authors), CoreMatchers.<Set<String>>equalTo(new HashSet<>(Arrays.asList("A@DOMAIN", "B@DOMAIN", "C@DOMAIN"))));
    }

    @Test
    public void getUsersTriggeringTheBuilds() throws Exception {

    }

    @Test
    public void getUserTriggeringTheBuild() throws Exception {

    }

    @Test
    public void addUsers() throws Exception {

    }

}
