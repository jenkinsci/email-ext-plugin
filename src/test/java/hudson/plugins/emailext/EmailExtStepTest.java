package hudson.plugins.emailext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang.StringEscapeUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.mock_javamail.Mailbox;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by acearl on 9/15/2015.
 */
public class EmailExtStepTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @After
    public void tearDown() {
        Mailbox.clearAll();
    }

    @Test
    public void configRoundTrip() throws Exception {
        EmailExtStep step1 = new EmailExtStep("subject", "body");
        step1.setTo("mickeymouse@disney.com");
        step1.setReplyTo("mickeymouse@disney.com");
        step1.setMimeType("text/html");

        EmailExtStep step2 = new StepConfigTester(j).configRoundTrip(step1);
        j.assertEqualDataBoundBeans(step1, step2);
    }

    @Test
    public void simpleEmail() throws Exception {
        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "wf");
        job.setDefinition(
                new CpsFlowDefinition("node { emailext(to: 'mickeymouse@disney.com', subject: 'Boo') }", true));
        Run<?, ?> run = job.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(run);

        Mailbox mbox = Mailbox.get("mickeymouse@disney.com");
        assertEquals(1, mbox.size());
        Message msg = mbox.get(0);
        assertEquals("Boo", msg.getSubject());
    }

    @Test
    public void attachLog() throws Exception {
        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "wf");
        job.setDefinition(new CpsFlowDefinition(
                "node { emailext(to: 'mickeymouse@disney.com', subject: 'Boo', attachLog: true) }", true));
        Run<?, ?> run = job.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(run);

        Mailbox mbox = Mailbox.get("mickeymouse@disney.com");
        assertEquals(1, mbox.size());
        Message msg = mbox.get(0);
        assertEquals("Boo", msg.getSubject());

        assertTrue("Message should be multipart", msg instanceof MimeMessage);
        assertTrue("Content should be a MimeMultipart", msg.getContent() instanceof MimeMultipart);

        MimeMultipart part = (MimeMultipart) msg.getContent();

        assertEquals("Should have two body items (message + attachment)", 2, part.getCount());

        BodyPart attach = part.getBodyPart(1);
        assertTrue(
                "There should be a log named \"build.log\" attached",
                "build.log".equalsIgnoreCase(attach.getFileName()));
    }

    @Test
    public void attachment() throws Exception {
        URL url = this.getClass().getResource("/test.pdf");
        final File attachment = new File(url.getFile());

        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "wf");
        job.setDefinition(new CpsFlowDefinition(
                "node { fileCopy('" + StringEscapeUtils.escapeJava(attachment.getAbsolutePath())
                        + "'); emailext (to: 'mickeymouse@disney.com', subject: 'Boo', body: 'Here is your file', attachmentsPattern: '*.pdf') }",
                true));
        Run<?, ?> run = job.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(run);

        Mailbox mbox = Mailbox.get("mickeymouse@disney.com");
        assertEquals("Should have an email from success", 1, mbox.size());

        Message msg = mbox.get(0);
        assertTrue("Message should be multipart", msg instanceof MimeMessage);
        assertTrue("Content should be a MimeMultipart", msg.getContent() instanceof MimeMultipart);

        MimeMultipart part = (MimeMultipart) msg.getContent();

        assertEquals("Should have two body items (message + attachment)", 2, part.getCount());

        BodyPart attach = part.getBodyPart(1);
        assertTrue(
                "There should be a PDF named \"test.pdf\" attached", "test.pdf".equalsIgnoreCase(attach.getFileName()));
    }

    @Test
    public void saveOutput() throws Exception {
        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "wf");
        job.setDefinition(new CpsFlowDefinition(
                """
                node {
                  emailext(to: 'mickeymouse@disney.com', subject: 'Boo', saveOutput: true)
                  archiveArtifacts '*.*'
                }\
                """,
                true));
        Run<?, ?> run = job.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(run);

        Mailbox mbox = Mailbox.get("mickeymouse@disney.com");
        assertEquals(1, mbox.size());
        Message msg = mbox.get(0);
        assertEquals("Boo", msg.getSubject());
        j.assertLogContains("Archiving artifacts", run);
    }

    @Test
    public void testErrorHandlingForTokenMacros() throws Exception {
        assertThat(
                getLogForMacroPipeline("any", "${FILE, path=\"no.txt\"}"),
                containsString("File 'no.txt' does not exist"));
        assertThat(
                getLogForMacroPipeline("none", "${FILE, path=\"no.txt\"}"),
                containsString("Macro 'FILE' can ony be evaluated in a workspace."));
        assertThat(
                getLogForMacroPipeline("any", "${TEMPLATE, file=\"no.txt\"}"),
                containsString("Text file [no.txt] was not found in $JENKINS_HOME/email-templates"));
        assertThat(
                getLogForMacroPipeline("none", "${TEMPLATE, file=\"no.txt\"}"),
                containsString("Text file [no.txt] was not found in $JENKINS_HOME/email-templates"));
    }

    private String getLogForMacroPipeline(String agent, String macro) throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "bar" + agent + macro.hashCode());
        job.setDefinition(new CpsFlowDefinition(getPipeline(agent, macro), true));
        j.assertBuildStatus(Result.SUCCESS, job.scheduleBuild2(0));
        Mailbox mbox = Mailbox.get("mickeymouse@disney.com");
        assertEquals(1, mbox.size());
        Message msg = mbox.get(0);
        Mailbox.clearAll();
        return (String) ((MimeMultipart) msg.getContent()).getBodyPart(0).getContent();
    }

    private String getPipeline(String agent, String macro) {
        return "pipeline {\n" + "  agent "
                + agent + "\n" + "  stages {\n"
                + "    stage('Cool') {\n"
                + "      steps {\n"
                + "       emailext(to: 'mickeymouse@disney.com', subject: 'Boo', body: '"
                + macro + "')" + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";
    }

    public static class FileCopyStep extends Step {

        private final String file;

        @DataBoundConstructor
        public FileCopyStep(String file) {
            this.file = file;
        }

        public String getFile() {
            return file;
        }

        @Override
        public StepExecution start(StepContext context) {
            return new Execution(this, context);
        }

        @TestExtension("attachment")
        public static class DescriptorImpl extends StepDescriptor {

            @Override
            public Set<? extends Class<?>> getRequiredContext() {
                return Collections.singleton(FilePath.class);
            }

            @Override
            public String getFunctionName() {
                return "fileCopy";
            }

            @NonNull
            @Override
            public String getDisplayName() {
                return "Copy a file into the workspace";
            }
        }

        public static class Execution extends SynchronousNonBlockingStepExecution<Boolean> {

            private final transient FileCopyStep step;

            protected Execution(FileCopyStep step, @NonNull StepContext context) {
                super(context);
                this.step = step;
            }

            @Override
            protected Boolean run() throws Exception {
                FilePath file = new FilePath(new File(step.file));
                getContext().get(FilePath.class).child(file.getName()).copyFrom(file);
                return true;
            }

            private static final long serialVersionUID = 1L;
        }
    }
}
