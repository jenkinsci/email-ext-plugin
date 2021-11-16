 package hudson.plugins.emailext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause.UserIdCause;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.RequesterRecipientProvider;
import hudson.plugins.emailext.plugins.trigger.AbortedTrigger;
import hudson.plugins.emailext.plugins.trigger.AlwaysTrigger;
import hudson.plugins.emailext.plugins.trigger.FailureTrigger;
import hudson.plugins.emailext.plugins.trigger.FirstFailureTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedUnhealthyTrigger;
import hudson.plugins.emailext.plugins.trigger.NotBuiltTrigger;
import hudson.plugins.emailext.plugins.trigger.PreBuildTrigger;
import hudson.plugins.emailext.plugins.trigger.RegressionTrigger;
import hudson.plugins.emailext.plugins.trigger.StillFailingTrigger;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import hudson.security.AuthorizationStrategy;
import hudson.security.SecurityRealm;
import hudson.tasks.Builder;
import hudson.tasks.Mailer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.mock_javamail.Mailbox;
import org.kohsuke.stapler.Stapler;

public class ExtendedEmailPublisherTest {

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();

    private ExtendedEmailPublisher publisher;
    private FreeStyleProject project;
    private List<RecipientProvider> recProviders;

    @ClassRule public static JenkinsRule j = new JenkinsRule();
    private AuthorizationStrategy oldAuthorizationStrategy;
    private SecurityRealm oldSecurityRealm;
    private String oldAdminAddress;
    @Rule public TestName testName = new TestName();

    @Before
    public void setUp() throws Exception {
        publisher = new ExtendedEmailPublisher();
        publisher.from = "";
        publisher.contentType = "default";
        publisher.defaultSubject = "$DEFAULT_SUBJECT";
        publisher.defaultContent = "$DEFAULT_CONTENT";
        publisher.attachmentsPattern = "";
        publisher.recipientList = "$DEFAULT_RECIPIENTS";
        publisher.setPresendScript("$DEFAULT_PRESEND_SCRIPT");
        publisher.setPostsendScript("$DEFAULT_POSTSEND_SCRIPT");
        publisher.replyTo = "$DEFAULT_REPLYTO";

        project = j.createFreeStyleProject(testName.getMethodName());
        project.getPublishersList().add(publisher);

        recProviders = Collections.emptyList();

        publisher.getDescriptor().setDefaultClasspath(Collections.emptyList());
        publisher.getDescriptor().setDefaultSuffix(null);
        publisher.getDescriptor().setEmergencyReroute(null);
        publisher.getDescriptor().setAllowedDomains(null);
        oldAuthorizationStrategy = j.jenkins.getAuthorizationStrategy();
        oldSecurityRealm = j.jenkins.getSecurityRealm();
        oldAdminAddress = JenkinsLocationConfiguration.get().getAdminAddress();
    }

    @After
    public void tearDown() throws Exception {
        Mailbox.clearAll();
        j.jenkins.setAuthorizationStrategy(oldAuthorizationStrategy);
        j.jenkins.setSecurityRealm(oldSecurityRealm);
        JenkinsLocationConfiguration.get().setAdminAddress(oldAdminAddress);
        ScriptApproval approval = ScriptApproval.get();
        approval.clearApprovedClasspathEntries();
        approval.clearApprovedScripts();
    }

    private void setUpSecurity() {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                                          // otherwise we would need to create users for each email address tested, to bypass SECURITY-372 fix:
                                          .grant(Jenkins.READ, Item.READ).everywhere().toAuthenticated()
                                          // TODO I had plans for tests where bob would approve scripts written by alice
                                          .grant(Jenkins.ADMINISTER).everywhere().to("bob")
                                          .grant(Item.EXTENDED_READ).everywhere().to("alice"));
    }

    private void setupApprovedClassPath(String resource) throws IOException {
        GroovyScriptPath path = new GroovyScriptPath(resource);
        publisher.setClasspath(Collections.singletonList(path));
        ScriptApproval approval = ScriptApproval.get();
        List<ScriptApproval.PendingClasspathEntry> entries = approval.getPendingClasspathEntries();
        assertThat(entries, not(empty()));
        for (ScriptApproval.PendingClasspathEntry entry : entries) {
            approval.approveClasspathEntry(entry.getHash());
        }
        assertThat(approval.getPendingClasspathEntries(), empty());
    }

    private void setupApprovedGlobalClassPath(String resource) throws IOException, Descriptor.FormException {
        GroovyScriptPath path = new GroovyScriptPath(resource);
        publisher.getDescriptor().setDefaultClasspath(Collections.singletonList(path));
        ScriptApproval approval = ScriptApproval.get();
        List<ScriptApproval.PendingClasspathEntry> entries = approval.getPendingClasspathEntries();
        assertThat(entries, not(empty()));
        for (ScriptApproval.PendingClasspathEntry entry : entries) {
            approval.approveClasspathEntry(entry.getHash());
        }
        assertThat(approval.getPendingClasspathEntries(), empty());
    }

    @Test
    public void testShouldNotSendEmailWhenNoTriggerEnabled()
            throws Exception {
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        List<String> log = build.getLog(100);
        assertThat("No emails should have been trigger during pre-build or post-build.", log,
                hasItems("No emails were triggered.", "No emails were triggered."));
    }

    @Test
    public void testPreBuildTriggerShouldAlwaysSendEmail()
            throws Exception {
        PreBuildTrigger trigger = new PreBuildTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertThat("Email should have been triggered, so we should see it in the logs.", build.getLog(100),
                hasItems("Email was triggered for: " + PreBuildTrigger.TRIGGER_NAME));
        assertEquals(1, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testPreBuildTriggerShouldAlwaysSendEmailSecured() throws Exception {
        setUpSecurity();
        testPreBuildTriggerShouldAlwaysSendEmail();
    }

    @Test
    public void testSuccessTriggerShouldSendEmailWhenBuildSucceeds()
            throws Exception {
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(successTrigger);
        publisher.getConfiguredTriggers().add(successTrigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertThat("Email should have been triggered, so we should see it in the logs.", build.getLog(100),
                hasItems("Email was triggered for: Success"));
        assertEquals(1, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testSuccessTriggerShouldNotSendEmailWhenBuildFails()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        SuccessTrigger trigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        assertThat("Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog(100),
                not(hasItems("Email was triggered for: " + SuccessTrigger.TRIGGER_NAME)));
        assertEquals(0, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testFirstFailureTriggerShouldNotSendEmailOnSecondFail()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        FirstFailureTrigger trigger = new FirstFailureTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        FreeStyleBuild build2 = project.scheduleBuild2(1).get();
        j.assertBuildStatus(Result.FAILURE, build2);

        assertThat("Email should have been triggered for build 0, so we should see it in the logs.", build.getLog(100),
                hasItems("Email was triggered for: " + FirstFailureTrigger.TRIGGER_NAME));

        assertThat("Email should NOT have been triggered for build 1, so we shouldn't see it in the logs.", build2.getLog(100),
                not(hasItems("Email was triggered for: " + FailureTrigger.TRIGGER_NAME)));
        assertEquals(1, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testFixedTriggerShouldNotSendEmailWhenBuildFirstFails()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        FixedTrigger trigger = new FixedTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        assertThat("Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog(100),
                not(hasItems("Email was triggered for: " + SuccessTrigger.TRIGGER_NAME)));
        assertEquals("No email should have been sent out since the build failed only once.", 0,
                Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testFixedTriggerShouldSendEmailWhenBuildIsFixed()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        FixedTrigger trigger = new FixedTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build1 = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build1);

        project.getBuildersList().clear();
        FreeStyleBuild build2 = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build2);

        assertThat("Email should have been triggered, so we should see it in the logs.", build2.getLog(100),
                hasItems("Email was triggered for: " + FixedTrigger.TRIGGER_NAME));
        assertEquals(1, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testFixedTriggerShouldNotSendEmailWhenBuildSucceedsAfterAbortedBuild()
            throws Exception {
        // fail
        project.getBuildersList().add(new FailureBuilder());
        FreeStyleBuild build1 = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build1);

        // abort
        project.getBuildersList().clear();
        project.getBuildersList().add(new MockBuilder(Result.ABORTED));
        FreeStyleBuild build2 = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.ABORTED, build2);

        FixedTrigger trigger = new FixedTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        // succeed
        project.getBuildersList().clear();
        FreeStyleBuild build3 = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build3);

        assertThat("Email should not have been triggered, so we shouldn't see it in the logs.", build3.getLog(100),
                not(hasItems("Email was triggered for: " + SuccessTrigger.TRIGGER_NAME)));
        assertEquals("No email should have been sent out since the prior build was aborted.", 0,
                Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testFixedUnhealthyTriggerShouldNotSendEmailWhenBuildFirstFails()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        FixedUnhealthyTrigger trigger = new FixedUnhealthyTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        assertThat("Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog(100),
                not(hasItems("Email was triggered for: " + SuccessTrigger.TRIGGER_NAME)));
        assertEquals("No email should have been sent out since the build failed only once.", 0,
                Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testFixedUnhealthyTriggerShouldSendEmailWhenBuildIsFixed()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        FixedUnhealthyTrigger trigger = new FixedUnhealthyTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build1 = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build1);

        project.getBuildersList().clear();
        FreeStyleBuild build2 = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build2);

        assertThat("Email should have been triggered, so we should see it in the logs.", build2.getLog(100),
                hasItems("Email was triggered for: " + FixedUnhealthyTrigger.TRIGGER_NAME));
        assertEquals(1, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testFixedUnhealthyTriggerShouldSendEmailWhenBuildSucceedsAfterAbortedBuild()
            throws Exception {
        // fail
        project.getBuildersList().add(new FailureBuilder());
        FreeStyleBuild build1 = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build1);

        // abort
        project.getBuildersList().clear();
        project.getBuildersList().add(new MockBuilder(Result.ABORTED));
        FreeStyleBuild build2 = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.ABORTED, build2);

        FixedUnhealthyTrigger trigger = new FixedUnhealthyTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        // succeed
        project.getBuildersList().clear();
        FreeStyleBuild build3 = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build3);

        assertThat("Email should have been triggered, so we should see it in the logs.", build3.getLog(100),
                hasItems("Email was triggered for: " + FixedUnhealthyTrigger.TRIGGER_NAME));
        assertEquals(1, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testStillFailingTriggerShouldNotSendEmailWhenBuildSucceeds()
            throws Exception {
        StillFailingTrigger trigger = new StillFailingTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertThat("Email should not have been triggered, so we should not see it in the logs.", build.getLog(100),
                not(hasItems("Email was triggered for: " + StillFailingTrigger.TRIGGER_NAME)));
        assertEquals(0, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testStillFailingTriggerShouldNotSendEmailWhenBuildFirstFails()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        StillFailingTrigger trigger = new StillFailingTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        // only fail once
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        assertThat("Email should not have been triggered, so we should not see it in the logs.", build.getLog(100),
                not(hasItems("Email was triggered for: " + StillFailingTrigger.TRIGGER_NAME)));
        assertEquals(0, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testStillFailingTriggerShouldNotSendEmailWhenBuildIsFixed()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        StillFailingTrigger trigger = new StillFailingTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        // only fail once
        FreeStyleBuild build1 = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build1);
        // then succeed
        project.getBuildersList().clear();
        FreeStyleBuild build2 = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build2);

        assertThat("Email should not have been triggered, so we should not see it in the logs.", build2.getLog(100),
                not(hasItems("Email was triggered for: " + StillFailingTrigger.TRIGGER_NAME)));
        assertEquals(0, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testStillFailingTriggerShouldSendEmailWhenBuildContinuesToFail()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        StillFailingTrigger trigger = new StillFailingTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        // first failure
        FreeStyleBuild build1 = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build1);
        // second failure
        FreeStyleBuild build2 = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build2);

        assertThat("Email should have been triggered, so we should see it in the logs.", build2.getLog(100),
                hasItems("Email was triggered for: " + StillFailingTrigger.TRIGGER_NAME));
        assertEquals("We should only have one email since the first failure doesn't count as 'still failing'.", 1,
                Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testAbortedTriggerShouldSendEmailWhenBuildAborts()
            throws Exception {
        project.getBuildersList().add(new MockBuilder(Result.ABORTED));

        AbortedTrigger abortedTrigger = new AbortedTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(abortedTrigger);
        publisher.getConfiguredTriggers().add(abortedTrigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.ABORTED, build);

        assertThat("Email should have been triggered, so we should see it in the logs.", build.getLog(100),
                hasItems("Email was triggered for: " + AbortedTrigger.TRIGGER_NAME));
        assertEquals(1, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testAbortedTriggerShouldNotSendEmailWhenBuildFails()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        AbortedTrigger trigger = new AbortedTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        assertThat("Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog(100),
                not(hasItems("Email was triggered for: " + AbortedTrigger.TRIGGER_NAME)));
        assertEquals(0, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testNotBuiltTriggerShouldSendEmailWhenNotBuilt()
            throws Exception {
        project.getBuildersList().add(new MockBuilder(Result.NOT_BUILT));

        NotBuiltTrigger notbuiltTrigger = new NotBuiltTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(notbuiltTrigger);
        publisher.getConfiguredTriggers().add(notbuiltTrigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.NOT_BUILT, build);

        assertThat("Email should have been triggered, so we should see it in the logs.", build.getLog(100),
                hasItems("Email was triggered for: " + NotBuiltTrigger.TRIGGER_NAME));
        assertEquals(1, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testNotBuiltTriggerShouldNotSendEmailWhenBuildFails()
            throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        NotBuiltTrigger trigger = new NotBuiltTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        assertThat("Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog(100),
                not(hasItems("Email was triggered for: " + NotBuiltTrigger.TRIGGER_NAME)));
        assertEquals(0, Mailbox.get("ashlux@gmail.com").size());
    }

    @Test
    public void testShouldSendEmailUsingUtf8ByDefault() throws Exception {
        project.getBuildersList().add(new FailureBuilder());

        FailureTrigger trigger = new FailureTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        Mailbox mailbox = Mailbox.get("ashlux@gmail.com");
        assertEquals("We should an email since the build failed.", 1, mailbox.size());
        Message msg = mailbox.get(0);
        assertThat("Message should be multipart", msg.getContentType(),
                containsString("multipart/mixed"));

        // TODO: add more tests for getting the multipart information.
        if (msg instanceof MimeMessage) {
            MimeMessage mimeMsg = (MimeMessage) msg;
            assertEquals("Message content should be a MimeMultipart instance",
                    MimeMultipart.class, mimeMsg.getContent().getClass());
            MimeMultipart multipart = (MimeMultipart) mimeMsg.getContent();
            assertTrue("There should be at least one part in the email",
                    multipart.getCount() >= 1);
            MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(0);
            assertThat("UTF-8 charset should be used.", bodyPart.getContentType(),
                    containsString("charset=UTF-8"));
        } else {
            assertThat("UTF-8 charset should be used.", mailbox.get(0).getContentType(),
                    containsString("charset=UTF-8"));
        }
    }

    @Test
    public void testCancelFromPresendScriptCausesNoEmail() throws Exception {
        publisher.setPresendScript("cancel = true");
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(0, Mailbox.get("kutzi@xxx.com").size());
    }

    @Test
    public void testCancelFromPresendScriptCausesNoEmailSecured() throws Exception {
        setUpSecurity();
        testCancelFromPresendScriptCausesNoEmail();
    }
    
    @Test
    @Issue("JENKINS-27448")
    public void testCancelFromPresendScriptCausesNoEmailWithCodeAfter() throws Exception {
        publisher.setPresendScript("cancel = true\nlogger.println('You are here')");
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);
        j.assertLogContains("You are here", build);
        assertEquals(0, Mailbox.get("kutzi@xxx.com").size());
    }

    @Test
    @Issue("JENKINS-27448")
    public void testCancelFromPresendScriptCausesNoEmailWithCodeAfterSecured() throws Exception {
        setUpSecurity();
        testCancelFromPresendScriptCausesNoEmailWithCodeAfter();
    }

    @Test
    @Issue("JENKINS-22777")
    public void testEmergencyRerouteOverridesPresendScript() throws Exception {
        publisher.getDescriptor().setEmergencyReroute("emergency@foo.com");
        publisher.setPresendScript("import javax.mail.Message.RecipientType\n"
                + "msg.setRecipients(RecipientType.TO, 'slide.o.mix@xxx.com')");
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(0, Mailbox.get("kutzi@xxx.com").size());
        assertEquals(0, Mailbox.get("slide.o.mix@xxx.com").size());
        assertEquals(1, Mailbox.get("emergency@foo.com").size());
    }

    @Test
    public void testNoCancelFromPresendScriptCausesEmail() throws Exception {
        publisher.setPresendScript("def hello = 'world'\n");
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(1, Mailbox.get("kutzi@xxx.com").size());
    }

    @Test
    public void testNoCancelFromPresendScriptCausesEmailSecure() throws Exception {
        setUpSecurity();
        testNoCancelFromPresendScriptCausesEmail();
    }

    @Test
    public void testPresendScriptModifiesTo() throws Exception {
        publisher.setPresendScript("import javax.mail.Message.RecipientType\n"
                + "msg.setRecipients(RecipientType.TO, 'slide.o.mix@xxx.com')");
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(0, Mailbox.get("kutzi@xxx.com").size());
        assertEquals(1, Mailbox.get("slide.o.mix@xxx.com").size());
    }

    @Test
    public void testPresendScriptModifiesToSecured() throws Exception {
        setUpSecurity();
        testPresendScriptModifiesTo();
    }

    @Test
    public void testPresendScriptModifiesToUsingProjectExternalScript() throws Exception {
        publisher.setClasspath(Collections.singletonList(new GroovyScriptPath("src/test/presend")));
        verifyPresendScriptModifiesToUsingProjectExternalScript();
    }

    @Test
    public void testPresendScriptModifiesToUsingProjectExternalScriptSecured() throws Exception {
        setUpSecurity();
        setupApprovedClassPath("src/test/resources/email-ext-test-helper-0.1.jar");
        verifyPresendScriptModifiesToUsingProjectExternalScript();
    }

    private void verifyPresendScriptModifiesToUsingProjectExternalScript() throws Exception {
        publisher.setPresendScript("import javax.mail.Message.RecipientType\n"
                + "import hudson.plugins.emailext.ExtendedEmailPublisherTestHelper\n"
                + "msg.setRecipients(RecipientType.TO, ExtendedEmailPublisherTestHelper.to())");
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                                                           "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT",
                                                           "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(0, Mailbox.get("kutzi@xxx.com").size());
        assertEquals(1, Mailbox.get("slide.o.mix@xxx.com").size());
    }


    @Test
    public void testPresendScriptModifiesToUsingGlobalExternalScript() throws Exception {
        publisher.getDescriptor().setDefaultClasspath(Collections.singletonList(new GroovyScriptPath("src/test/presend")));
        verifyPresendScriptModifiesToUsingGlobalExternalScript();
    }

    @Test
    public void testPresendScriptModifiesToUsingGlobalExternalScriptSecure() throws Exception {
        setUpSecurity();
        setupApprovedGlobalClassPath("src/test/resources/email-ext-test-helper-0.1.jar");
        verifyPresendScriptModifiesToUsingGlobalExternalScript();
    }

    private void verifyPresendScriptModifiesToUsingGlobalExternalScript() throws Exception {
        publisher.setPresendScript("import javax.mail.Message.RecipientType\n"
                + "import hudson.plugins.emailext.ExtendedEmailPublisherTestHelper\n"
                + "msg.setRecipients(RecipientType.TO, ExtendedEmailPublisherTestHelper.to())");
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                                                           "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT",
                                                           "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(0, Mailbox.get("kutzi@xxx.com").size());
        assertEquals(1, Mailbox.get("slide.o.mix@xxx.com").size());
    }

    @Test
    public void testPostsendScriptModifiesMessageId() throws Exception {
        publisher.setPostsendScript("msg.setHeader('Message-ID', '<12345@xxx.com>')");

        verifyPostsendScriptModifiesMessageId();
    }

    @Test
    public void testPostsendScriptModifiesMessageIdUsingProjectExternalScript() throws Exception {
        publisher.setClasspath(Collections.singletonList(new GroovyScriptPath("src/test/postsend")));
        verifyPostsendScriptModifiesMessageIdUsingProjectExternalScript();
    }

    @Test
    public void testPostsendScriptModifiesMessageIdUsingProjectExternalScriptSecure() throws Exception {
        setUpSecurity();
        setupApprovedClassPath("src/test/resources/email-ext-test-helper-0.1.jar");
        verifyPostsendScriptModifiesMessageIdUsingProjectExternalScript();
    }

    private void verifyPostsendScriptModifiesMessageIdUsingProjectExternalScript() throws Exception {
        publisher.setPostsendScript("import hudson.plugins.emailext.ExtendedEmailPublisherTestHelper\n"
                + "msg.setHeader('Message-ID', ExtendedEmailPublisherTestHelper.messageid())");
        verifyPostsendScriptModifiesMessageId();
    }

    private void verifyPostsendScriptModifiesMessageId() throws Exception {
        project.getBuildersList().add(new FailureBuilder());
        FailureTrigger trigger = new FailureTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                                                    "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT",
                                                    "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        FreeStyleBuild build2 = project.scheduleBuild2(1).get();
        j.assertBuildStatus(Result.FAILURE, build2);

        Mailbox mailbox = Mailbox.get("ashlux@gmail.com");
        assertEquals(2, mailbox.size());

        Message msg = mailbox.get(1);

        assertEquals("<12345@xxx.com>", getHeader(msg, "In-Reply-To"));
    }

    @Test
    public void testPostsendScriptModifiesToUsingGlobalExternalScript() throws Exception {
        publisher.setPostsendScript("import hudson.plugins.emailext.ExtendedEmailPublisherTestHelper\n"
                + "msg.setHeader('Message-ID', ExtendedEmailPublisherTestHelper.messageid())");
        List<GroovyScriptPath> classpath = new ArrayList<>();
        classpath.add(new GroovyScriptPath("src/test/postsend"));
        publisher.getDescriptor().setDefaultClasspath(classpath);

        verifyPostsendScriptModifiesMessageId();
    }

    @Test
    public void testSendToRequesterLegacy() throws Exception {
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(1, Mailbox.get("kutzi@xxx.com").size());
    }

    @Test
    public void testReplyTo() throws Exception {
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);
        publisher.replyTo = "ashlux@gmail.com";

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);

        Mailbox mailbox = Mailbox.get("kutzi@xxx.com");
        assertEquals(1, mailbox.size());

        Message msg = mailbox.get(0);
        Address[] replyTo = msg.getReplyTo();
        assertEquals(1, replyTo.length);

        assertEquals("ashlux@gmail.com", replyTo[0].toString());
    }

    @Test
    public void testNoReplyTo() throws Exception {
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatusSuccess(build);

        Mailbox mailbox = Mailbox.get("kutzi@xxx.com");
        assertEquals(1, mailbox.size());

        Message msg = mailbox.get(0);
        Address[] replyTo = msg.getReplyTo();
        assertEquals(1, replyTo.length);

        assertEquals("address not configured yet <nobody@nowhere>", replyTo[0].toString());
    }

    @Test
    public void testNewInstance_shouldGetBasicInformation()
            throws Exception {
        j.createWebClient().executeOnServer(() -> {
            JSONObject form = new JSONObject();
            form.put("project_content_type", "default");
            form.put("project_recipient_list", "ashlux@gmail.com");
            form.put("project_default_subject", "Make millions in Nigeria");
            form.put("project_default_content", "Give me a $1000 check and I'll mail you back $5000!!!");
            form.put("project_attachments", "");
            form.put("project_presend_script", "");
            form.put("postsendScript", "println 1");
            form.put("project_replyto", "");

            ExtendedEmailPublisherDescriptor descriptor = new ExtendedEmailPublisherDescriptor();
            publisher = (ExtendedEmailPublisher) descriptor.newInstance(Stapler.getCurrentRequest(), form);

            assertEquals("default", publisher.contentType);
            assertEquals("ashlux@gmail.com", publisher.recipientList);
            assertEquals("Make millions in Nigeria", publisher.defaultSubject);
            assertEquals("Give me a $1000 check and I'll mail you back $5000!!!", publisher.defaultContent);
            assertEquals("", publisher.attachmentsPattern);
            assertEquals("", publisher.replyTo);
            assertEquals("println 1", publisher.getPostsendScript());

            return null;
        });
    }

    @Issue("JENKINS-20524")
    @Test
    public void testMultipleTriggersOfSameType()
            throws Exception {
        FreeStyleProject prj = j.createFreeStyleProject("JENKINS-20524");
        prj.getPublishersList().add(publisher);

        publisher.recipientList = "mickey@disney.com";
        publisher.configuredTriggers.add(new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project"));
        publisher.configuredTriggers.add(new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project"));

        for (EmailTrigger trigger : publisher.configuredTriggers) {
            trigger.getEmail().addRecipientProvider(new ListRecipientProvider());
        }

        FreeStyleBuild build = prj.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(2, Mailbox.get("mickey@disney.com").size());
    }

    @Issue("JENKINS-22154")
    @Test
    public void testProjectDisable() throws Exception {
        FreeStyleProject prj = j.createFreeStyleProject("JENKINS-22154");
        prj.getPublishersList().add(publisher);

        publisher.disabled = true;
        publisher.recipientList = "mickey@disney.com";
        publisher.configuredTriggers.add(new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project"));

        for (EmailTrigger trigger : publisher.configuredTriggers) {
            trigger.getEmail().addRecipientProvider(new ListRecipientProvider());
        }

        FreeStyleBuild build = prj.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(0, Mailbox.get("mickey@disney.com").size());
        assertThat("Publisher is disabled, should have message in build log", build.getLog(100),
                hasItem("Extended Email Publisher is currently disabled in project settings"));
    }

    
    @Test
    @Issue("JENKINS-15442")
    public void testConfiguredStateNoTriggers()
            throws Exception {
        FreeStyleProject prj = j.createFreeStyleProject("JENKINS-15442");
        prj.getPublishersList().add(publisher);

        publisher.recipientList = "mickey@disney.com";
        publisher.configuredTriggers.clear();

        final WebClient client = j.createWebClient();
        final HtmlPage page = client.goTo("job/JENKINS-15442/configure");
        final HtmlTextArea recipientList = page.getElementByName("project_recipient_list");
        assertEquals("mickey@disney.com", recipientList.getText());
    }
    
    @Test
    @Issue("JENKINS-23126")
    public void testPlainTextAndHtml() throws Exception {
        FreeStyleProject prj = j.createFreeStyleProject("JENKINS-23126");
        prj.getPublishersList().add(publisher);      
        
        final String content = "<html><head><title>Foo</title></head><body><b>This is a test</b><br/>Hello world</body></html>";
        publisher.contentType = "both";
        publisher.recipientList = "mickey@disney.com";
        publisher.configuredTriggers.add(new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", content, "", 0, "project"));

        for (EmailTrigger trigger : publisher.configuredTriggers) {
            trigger.getEmail().addRecipientProvider(new ListRecipientProvider());
        }

        FreeStyleBuild build = prj.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(1, Mailbox.get("mickey@disney.com").size());
        
        Message msg = Mailbox.get("mickey@disney.com").get(0);
        assertTrue("Message should be multipart", msg instanceof MimeMessage);
        assertTrue("Content should be a MimeMultipart", msg.getContent() instanceof MimeMultipart);
        
        MimeMultipart part = (MimeMultipart)msg.getContent();
        assertEquals("Should have two body items (html + plaintext)", 2, part.getCount());  
        
        BodyPart plainText = part.getBodyPart(0);
        String plainTextString = IOUtils.toString(plainText.getInputStream(), publisher.getDescriptor().getCharset()).replace("\r", "");
        assertEquals("Should have the same plain text body", "This is a test\nHello world", plainTextString);
        
        BodyPart html = part.getBodyPart(1);
        String htmlString = IOUtils.toString(html.getInputStream(), publisher.getDescriptor().getCharset());
        
        assertEquals("Should have the same HTML body", content, htmlString);
    }
    
    @Issue("JENKINS-16376")
    @Test
    public void testConcurrentBuilds() throws Exception {
        publisher.configuredTriggers.add(new RegressionTrigger(recProviders, "", "", "", "", "", 0, ""));
        project.setConcurrentBuild(true);
        project.getBuildersList().add(new SleepOnceBuilder());
        FreeStyleBuild build1 = project.scheduleBuild2(0).waitForStart();
        assertEquals(1, build1.number);
        FreeStyleBuild build2 = j.assertBuildStatusSuccess(project.scheduleBuild2(0).get(9999, TimeUnit.MILLISECONDS));
        assertEquals(2, build2.number);
        assertTrue(build1.isBuilding());
        assertFalse(build2.isBuilding());
        j.assertLogContains(Messages.ExtendedEmailPublisher__is_still_in_progress_ignoring_for_purpo(build1.getDisplayName()), build2);
        build1.doStop();
        j.assertBuildStatus(Result.ABORTED, j.waitForCompletion(build1));
    }

    @Test
    public void testAttachBuildLog() throws Exception {
        publisher.attachBuildLog = true;
        AlwaysTrigger trigger = new AlwaysTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType(trigger);
        publisher.getConfiguredTriggers().add(trigger);
        AbstractBuild<?, ?> build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertThat("Email should have been triggered, so we should see it in the logs.", build.getLog(100),
                hasItems("Email was triggered for: " + AlwaysTrigger.TRIGGER_NAME));

        Mailbox mbox = Mailbox.get("ashlux@gmail.com");
        assertEquals(1, mbox.size());

        Message msg = mbox.get(0);

        assertTrue("Message should be multipart", msg instanceof MimeMessage);
        assertTrue("Content should be a MimeMultipart", msg.getContent() instanceof MimeMultipart);

        MimeMultipart part = (MimeMultipart) msg.getContent();

        assertEquals("Should have two body items (message + attachment)", 2, part.getCount());

        BodyPart attach = part.getBodyPart(1);
        assertTrue("There should be a log named \"build.log\" attached", "build.log".equalsIgnoreCase(attach.getFileName()));
    }

    @Test
    public void testAdditionalAccounts() throws Exception {
        j.createWebClient().executeOnServer(() -> {
            ExtendedEmailPublisherDescriptor descriptor = ExtendedEmailPublisher.descriptor();
            descriptor.getMailAccount().setSmtpHost("smtp.test0.com");
            descriptor.getMailAccount().setSmtpPort("587");
            descriptor.getMailAccount().setAdvProperties("mail.smtp.ssl.trust=test0.com");

            JSONObject form = new JSONObject();
            form.put("project_from", "mail@test1.com");
            publisher = (ExtendedEmailPublisher) descriptor.newInstance(Stapler.getCurrentRequest(), form);
            assertEquals("mail@test1.com", publisher.from);
            ExtendedEmailPublisherContext context =
                    new ExtendedEmailPublisherContext(publisher, null, null, null, TaskListener.NULL);
            Session session = descriptor.createSession(publisher.getMailAccount(context), context);
            assertEquals("smtp.test0.com", session.getProperty("mail.smtp.host"));
            assertEquals("587", session.getProperty("mail.smtp.port"));
            assertEquals("test0.com", session.getProperty("mail.smtp.ssl.trust"));

            List<MailAccount> addaccs = new ArrayList<>();
            JSONObject dform = new JSONObject();
            dform.put("address", "mail@test1.com");
            dform.put("smtpHost", "smtp.test1.com");
            dform.put("smtpPort", "25");
            dform.put("advProperties", "mail.smtp.ssl.trust=test1.com");
            addaccs.add(new MailAccount(dform));
            dform.put("address", "mail@test2.com");
            dform.put("smtpHost", "smtp.test2.com");
            dform.put("smtpPort", "465");
            dform.put("advProperties", "mail.smtp.ssl.trust=test2.com");
            addaccs.add(new MailAccount(dform));
            descriptor.setAddAccounts(addaccs);

            publisher = (ExtendedEmailPublisher) descriptor.newInstance(Stapler.getCurrentRequest(), form);
            assertEquals("mail@test1.com", publisher.from);
            session = descriptor.createSession(publisher.getMailAccount(context), context);
            assertEquals("smtp.test1.com", session.getProperty("mail.smtp.host"));
            assertEquals("25", session.getProperty("mail.smtp.port"));
            assertEquals("test1.com", session.getProperty("mail.smtp.ssl.trust"));

            form.put("project_from", "mail@test2.com");
            publisher = (ExtendedEmailPublisher) descriptor.newInstance(Stapler.getCurrentRequest(), form);
            assertEquals("mail@test2.com", publisher.from);
            session = descriptor.createSession(publisher.getMailAccount(context), context);
            assertEquals("smtp.test2.com", session.getProperty("mail.smtp.host"));
            assertEquals("465", session.getProperty("mail.smtp.port"));
            assertEquals("test2.com", session.getProperty("mail.smtp.ssl.trust"));

            return null;
        });
    }

    @Test
    public void testAllowedDomains1() throws Exception {
        FreeStyleProject prj = j.createFreeStyleProject();
        prj.getPublishersList().add(publisher);

        publisher.getDescriptor().setAllowedDomains("x1x.com,x2x.com");
        publisher.recipientList = "user1@x1x.com,user2@x2x.com,user3@foo.com,cc:user4@info.x1x.com,cc:user5@foo3.com,bcc:user6@foo1.com,bcc:user7@info.x2x.com";
        publisher.configuredTriggers.add(new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project"));

        for (EmailTrigger trigger : publisher.configuredTriggers) {
            trigger.getEmail().addRecipientProvider(new ListRecipientProvider());
        }

        FreeStyleBuild build = prj.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(1, Mailbox.get("user1@x1x.com").size());
        assertEquals(1, Mailbox.get("user2@x2x.com").size());
        assertEquals(0, Mailbox.get("user3@foo.com").size());
        assertEquals(1, Mailbox.get("user4@info.x1x.com").size());
        assertEquals(0, Mailbox.get("user5@foo3.com").size());
        assertEquals(0, Mailbox.get("user6@foo1.com").size());
        assertEquals(1, Mailbox.get("user7@info.x2x.com").size());
    }


    @Test
    public void testAllowedDomains2() throws Exception {
        FreeStyleProject prj = j.createFreeStyleProject();
        prj.getPublishersList().add(publisher);

        publisher.getDescriptor().setAllowedDomains("@x1x.com,@x2x.com");
        publisher.recipientList = "user1@x1x.com,user2@x2x.com,user3@foo.com,cc:user4@info.x1x.com,cc:user5@foo3.com,bcc:user6@foo1.com,bcc:user7@info.x2x.com";
        publisher.configuredTriggers.add(new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project"));

        for (EmailTrigger trigger : publisher.configuredTriggers) {
            trigger.getEmail().addRecipientProvider(new ListRecipientProvider());
        }

        FreeStyleBuild build = prj.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);

        assertEquals(1, Mailbox.get("user1@x1x.com").size());
        assertEquals(1, Mailbox.get("user2@x2x.com").size());
        assertEquals(0, Mailbox.get("user3@foo.com").size());
        assertEquals(0, Mailbox.get("user4@info.x1x.com").size());
        assertEquals(0, Mailbox.get("user5@foo3.com").size());
        assertEquals(0, Mailbox.get("user6@foo1.com").size());
        assertEquals(0, Mailbox.get("user7@info.x2x.com").size());
    }

    @Issue("SECURITY-1340")
    @Test
    public void testScriptConstructorsAreNotExecutedOutsideOfSandbox() throws Exception {
        setUpSecurity();

        publisher.setPresendScript("class DoNotRunConstructor {\n" +
            "  static void main(String[] args) {}\n" +
            "  DoNotRunConstructor() {\n" +
            "    assert jenkins.model.Jenkins.instance.createProject(hudson.model.FreeStyleProject, 'should-not-exist1')\n" +
            "  }\n" +
            "}\n");
        publisher.setPostsendScript("class DoNotRunConstructor {\n" +
            "  static void main(String[] args) {}\n" +
            "  DoNotRunConstructor() {\n" +
            "    assert jenkins.model.Jenkins.instance.createProject(hudson.model.FreeStyleProject, 'should-not-exist2')\n" +
            "  }\n" +
            "}\n");
        SuccessTrigger successTrigger = new SuccessTrigger(recProviders, "$DEFAULT_RECIPIENTS",
                "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType() {
            {
                addRecipientProvider(new RequesterRecipientProvider());
            }
        });
        publisher.getConfiguredTriggers().add(successTrigger);

        User u = User.getById("kutzi", true);
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);

        UserIdCause cause = new UserIdCause("kutzi");

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
        j.assertBuildStatus(Result.SUCCESS, build);
        j.assertLogContains("staticMethod jenkins.model.Jenkins getInstance", build);
        assertNull(j.jenkins.getItem("should-not-exist1"));
        assertNull(j.jenkins.getItem("should-not-exist2"));
    }

    @Issue("JENKINS-63846")
    @Test
    public void testSystemAdminEmailChange() throws Exception {
        SuccessTrigger successTrigger =
                new SuccessTrigger(
                        recProviders,
                        "$DEFAULT_RECIPIENTS",
                        "$DEFAULT_REPLYTO",
                        "$DEFAULT_SUBJECT",
                        "$DEFAULT_CONTENT",
                        "",
                        0,
                        "project");
        addEmailType(successTrigger);
        publisher.getConfiguredTriggers().add(successTrigger);

        JenkinsLocationConfiguration locationConfiguration = JenkinsLocationConfiguration.get();
        locationConfiguration.setAdminAddress("Foo <foo@example.com>");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("Email was triggered for: Success", build);

        locationConfiguration.setAdminAddress("Bar <bar@example.com>");
        build = j.buildAndAssertSuccess(project);
        j.assertLogContains("Email was triggered for: Success", build);

        Mailbox mailbox = Mailbox.get("ashlux@gmail.com");
        assertEquals(2, mailbox.size());

        Message message = mailbox.get(0);
        assertEquals("Foo <foo@example.com>", getHeader(message, "From"));
        message = mailbox.get(1);
        assertEquals("Bar <bar@example.com>", getHeader(message, "From"));
    }

    @Test
    public void testProjectFrom() throws Exception {
        SuccessTrigger successTrigger =
                new SuccessTrigger(
                        recProviders,
                        "$DEFAULT_RECIPIENTS",
                        "$DEFAULT_REPLYTO",
                        "$DEFAULT_SUBJECT",
                        "$DEFAULT_CONTENT",
                        "",
                        0,
                        "project");
        addEmailType(successTrigger);
        publisher.getConfiguredTriggers().add(successTrigger);
        publisher.from = "custom@example.com";

        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("Email was triggered for: Success", build);

        Mailbox mailbox = Mailbox.get("ashlux@gmail.com");
        assertEquals(1, mailbox.size());

        Message msg = mailbox.get(0);
        Address[] from = msg.getFrom();
        assertEquals(1, from.length);

        assertEquals("custom@example.com", from[0].toString());
    }

    @Test
    @Issue("JENKINS-63523")
    public void testProjectFromWithDefaultSuffix() throws Exception {
        publisher.getDescriptor().setDefaultSuffix("@example.com");

        SuccessTrigger successTrigger =
                new SuccessTrigger(
                        recProviders,
                        "$DEFAULT_RECIPIENTS",
                        "$DEFAULT_REPLYTO",
                        "$DEFAULT_SUBJECT",
                        "$DEFAULT_CONTENT",
                        "",
                        0,
                        "project");
        addEmailType(successTrigger);
        publisher.getConfiguredTriggers().add(successTrigger);
        publisher.from = "custom";

        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("Email was triggered for: Success", build);

        Mailbox mailbox = Mailbox.get("ashlux@gmail.com");
        assertEquals(1, mailbox.size());

        Message msg = mailbox.get(0);
        Address[] from = msg.getFrom();
        assertEquals(1, from.length);

        assertEquals("custom@example.com", from[0].toString());
    }

    /**
     * Similar to {@link SleepBuilder} but only on the first build. (Removing
     * the builder between builds is tricky since you would have to wait for the
     * first one to actually start it.)
     */
    private static final class SleepOnceBuilder extends Builder {

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
            if (build.number == 1) {
                Thread.sleep(99999);
            }
            return true;
        }

        public static final class DescriptorImpl extends Descriptor<Builder> {

            @NonNull
            @Override
            public String getDisplayName() {
                return "Sleep once";
            }
        }
    }

    private static void addEmailType(EmailTrigger trigger) {
        trigger.setEmail(new EmailType() {
            {
                setRecipientList("ashlux@gmail.com");
                setSubject("Yet another Hudson email");
                setBody("Boom goes the dynamite.");
            }
        });
    }

    private static String getHeader(Message message, String headerName) throws MessagingException {
        String[] headers = message.getHeader(headerName);
        assertNotNull(headers);
        assertEquals(1, headers.length);
        return headers[0];
    }
}
