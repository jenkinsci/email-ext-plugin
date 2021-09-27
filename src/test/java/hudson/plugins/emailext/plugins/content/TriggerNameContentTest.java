package hudson.plugins.emailext.plugins.content;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.trigger.PreBuildTrigger;
import java.util.Collections;
import java.util.List;
import javax.mail.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.mock_javamail.Mailbox;

/**
 *
 * @author acearl
 */
public class TriggerNameContentTest {
    private ExtendedEmailPublisher publisher;
    private FreeStyleProject project;

    @Rule public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        publisher = new ExtendedEmailPublisher();
        publisher.defaultSubject = "%DEFAULT_SUBJECT";
        publisher.defaultContent = "%DEFAULT_CONTENT";
        publisher.attachmentsPattern = "";
        publisher.recipientList = "%DEFAULT_RECIPIENTS";
        publisher.setPresendScript("");
        publisher.setPostsendScript("");

        project = j.createFreeStyleProject();
        project.getPublishersList().add(publisher);
    }

    @After
    public void tearDown() {
        Mailbox.clearAll();
    }

    
    @Test
    public void testTriggerName() throws Exception {
        List<RecipientProvider> recProviders = Collections.emptyList();
        PreBuildTrigger trigger = new PreBuildTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertThat("Email should have been triggered, so we should see it in the logs.", build.getLog(100),
                hasItems("Email was triggered for: " + PreBuildTrigger.TRIGGER_NAME));
        assertEquals(1, Mailbox.get("mickey@disney.com").size());
        Message message = Mailbox.get("mickey@disney.com").get(0);
        assertEquals(PreBuildTrigger.TRIGGER_NAME, message.getSubject());
    }
    
    private void addEmailType(EmailTrigger trigger) {
        trigger.setEmail(new EmailType() {
            {
                setRecipientList("mickey@disney.com");
                setSubject("${TRIGGER_NAME}");
            }
        });
    }
}
