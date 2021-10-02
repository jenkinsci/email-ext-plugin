package hudson.plugins.emailext.plugins.recipients;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;

public class RecipientProviderUtilitiesTest {
    public static class Debug implements RecipientProviderUtilities.IDebug {

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
    public void getChangeSetAuthors() {
        Debug debug = new Debug();
        WorkflowRun run1 = mock(WorkflowRun.class);
        Set<User> authors = RecipientProviderUtilities.getChangeSetAuthors(Collections.singleton(run1), debug);
        assertThat(authors, IsCollectionWithSize.hasSize(0));

        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet1 = MockUtilities.makeChangeSet(run1, "A");
        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet2 = MockUtilities.makeChangeSet(run1, "B");
        when(run1.getChangeSets()).thenReturn(Arrays.asList(changeSet1, changeSet2));

        authors = RecipientProviderUtilities.getChangeSetAuthors(Collections.singleton(run1), debug);
        assertThat(usersToEMails(authors), CoreMatchers.equalTo(new HashSet<>(Arrays.asList("A@DOMAIN", "B@DOMAIN"))));

        WorkflowRun run2 = mock(WorkflowRun.class);
        MockUtilities.addChangeSet(run2, "C");

        authors = RecipientProviderUtilities.getChangeSetAuthors(Arrays.asList(run1, run2), debug);
        assertThat(usersToEMails(authors), CoreMatchers.equalTo(new HashSet<>(Arrays.asList("A@DOMAIN", "B@DOMAIN", "C@DOMAIN"))));
    }

    @Test
    public void getUsersTriggeringTheBuilds() {

    }

    @Test
    public void getUserTriggeringTheBuild() {

    }

    @Test
    public void addUsers() {

    }

}
