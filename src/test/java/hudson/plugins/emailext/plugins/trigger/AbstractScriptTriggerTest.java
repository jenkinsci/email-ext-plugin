package hudson.plugins.emailext.plugins.trigger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Tests {@link PreBuildScriptTrigger} and {@link ScriptTrigger}.
 */
public class AbstractScriptTriggerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                // otherwise we would need to create users for each email address tested, to bypass SECURITY-372 fix:
                .grant(Jenkins.READ, Item.READ, Item.DISCOVER, Item.CONFIGURE).everywhere().toAuthenticated()
                .grant(Jenkins.ADMINISTER).everywhere().to("bob"));
    }

    @Test
    @Issue("SECURITY-257")
    public void configRoundtrip() throws Exception {
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.defaultSubject = "%DEFAULT_SUBJECT";
        publisher.defaultContent = "%DEFAULT_CONTENT";
        publisher.attachmentsPattern = "";
        publisher.recipientList = "%DEFAULT_RECIPIENTS";
        publisher.setPresendScript("");
        publisher.setPostsendScript("");

        final String script = "out.println('Checking before trigger')\n" +
                "return build.result.toString() == 'SUCCESS'";
        publisher.getConfiguredTriggers().add(new PreBuildScriptTrigger(
                Collections.emptyList(),
                "recipientList",
                "replyTo",
                "subject",
                "body",
                "attachmentsPattern",
                0,
                "text/plain",
                new SecureGroovyScript(script, true, null)));

        FreeStyleProject project = j.createFreeStyleProject("iMail");
        project.getPublishersList().add(publisher);

        JenkinsRule.WebClient client = j.createWebClient().login("alice");
        j.submit(client.getPage(project, "configure").getFormByName("config"));

        PreBuildScriptTrigger trigger = checkTrigger(PreBuildScriptTrigger.class, "iMail", Result.FAILURE, RejectedAccessException.class, false);

        assertEquals("recipientList", trigger.getEmail().getRecipientList());
        assertEquals("replyTo", trigger.getEmail().getReplyTo());
        assertEquals("subject", trigger.getEmail().getSubject());
        assertEquals("body", trigger.getEmail().getBody());
        assertEquals("text/plain", trigger.getEmail().getContentType());
        assertEquals(script, trigger.getSecureTriggerScript().getScript());
        assertTrue(trigger.getSecureTriggerScript().isSandbox());

        ScriptApproval.get().approveSignature("method hudson.model.Run getResult");

        project = j.jenkins.getItemByFullName("iMail", FreeStyleProject.class);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("Checking before trigger", build);

    }

    @Test
    @LocalData
    @Issue("SECURITY-257")
    public void beforeBuildTriggerMigration() throws Exception {
        checkTrigger(PreBuildScriptTrigger.class, "Before", Result.FAILURE, UnapprovedUsageException.class, true);

    }

    @Test
    @LocalData
    @Issue("SECURITY-257")
    public void afterBuildTriggerMigration() throws Exception {
        checkTrigger(ScriptTrigger.class, "After", Result.SUCCESS, UnapprovedUsageException.class, true);
    }

    @Test
    @Issue("SECURITY-1340")
    public void doNotExecuteConstructorsOutsideOfSandbox() throws Exception {
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.defaultSubject = "%DEFAULT_SUBJECT";
        publisher.defaultContent = "%DEFAULT_CONTENT";
        publisher.attachmentsPattern = "";
        publisher.recipientList = "%DEFAULT_RECIPIENTS";
        publisher.setPresendScript("");
        publisher.setPostsendScript("");
        String script = "class DoNotRunConstructor {\n" +
            "  static void main(String[] args) {}\n" +
            "  DoNotRunConstructor() {\n" +
            "    assert jenkins.model.Jenkins.instance.createProject(hudson.model.FreeStyleProject, 'should-not-exist')\n" +
            "  }\n" +
            "}\n";
        publisher.getConfiguredTriggers().add(new PreBuildScriptTrigger(
                Collections.emptyList(),
                "recipientList",
                "replyTo",
                "subject",
                "body",
                "attachmentsPattern",
                0,
                "text/plain",
                new SecureGroovyScript(script, true, null)));

        FreeStyleProject p = j.createFreeStyleProject("p1");
        p.getPublishersList().add(publisher);

        checkTrigger(PreBuildScriptTrigger.class, "p1", Result.FAILURE, RejectedAccessException.class, false);
        assertNull(j.jenkins.getItem("should-not-exist"));
    }

    private <T> T checkTrigger(Class<T> clazz, String name, Result firstStatus, Class<? extends Exception> expected, boolean approveSignature) throws Exception {
        FreeStyleProject project = j.jenkins.getItemByFullName(name, FreeStyleProject.class);
        assertNotNull(project);
        ExtendedEmailPublisher publisher = project.getPublishersList().get(ExtendedEmailPublisher.class);
        assertNotNull(publisher);
        List<EmailTrigger> triggers = publisher.getConfiguredTriggers();
        assertThat(triggers, hasSize(1));
        EmailTrigger trigger = triggers.get(0);
        assertThat(trigger, instanceOf(clazz));

        AbstractScriptTrigger at = (AbstractScriptTrigger) trigger;
        assertNotNull(at.getSecureTriggerScript());
        assertThat(at.getSecureTriggerScript().getScript(), not(emptyString()));

        FreeStyleBuild build = j.assertBuildStatus(firstStatus, project.scheduleBuild2(0));
        j.assertLogContains(expected.getName(), build); //TODO change whenever build.getResult() ends up in a whitelist

        if (approveSignature) {
            ScriptApproval.get().preapproveAll();

            build = j.buildAndAssertSuccess(project);
            j.assertLogContains("Checking " + name + " trigger", build);
        }

        return clazz.cast(at);
    }

}