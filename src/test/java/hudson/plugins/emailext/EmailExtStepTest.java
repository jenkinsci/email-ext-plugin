package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.Run;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.File;
import java.io.Serial;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.text.StringEscapeUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.mock_javamail.Mailbox;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by acearl on 9/15/2015.
 */
@WithJenkins
class EmailExtStepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @AfterEach
    void tearDown() {
        Mailbox.clearAll();
    }

    @Test
    void configRoundTrip() throws Exception {
        EmailExtStep step1 = new EmailExtStep("subject", "body");
        step1.setTo("mickeymouse@disney.com");
        step1.setReplyTo("mickeymouse@disney.com");
        step1.setMimeType("text/html");

        EmailExtStep step2 = new StepConfigTester(j).configRoundTrip(step1);
        j.assertEqualDataBoundBeans(step1, step2);
    }

    @Test
    void simpleEmail() throws Exception {
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
    void attachLog() throws Exception {
        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "wf");
        job.setDefinition(new CpsFlowDefinition(
                "node { emailext(to: 'mickeymouse@disney.com', subject: 'Boo', attachLog: true) }", true));
        Run<?, ?> run = job.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(run);

        Mailbox mbox = Mailbox.get("mickeymouse@disney.com");
        assertEquals(1, mbox.size());
        Message msg = mbox.get(0);
        assertEquals("Boo", msg.getSubject());

        assertInstanceOf(MimeMessage.class, msg, "Message should be multipart");
        assertInstanceOf(MimeMultipart.class, msg.getContent(), "Content should be a MimeMultipart");

        MimeMultipart part = (MimeMultipart) msg.getContent();

        assertEquals(2, part.getCount(), "Should have two body items (message + attachment)");

        BodyPart attach = part.getBodyPart(1);
        assertTrue(
                "build.log".equalsIgnoreCase(attach.getFileName()),
                "There should be a log named \"build.log\" attached");
    }

    @Test
    void attachment() throws Exception {
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
        assertEquals(1, mbox.size(), "Should have an email from success");

        Message msg = mbox.get(0);
        assertInstanceOf(MimeMessage.class, msg, "Message should be multipart");
        assertInstanceOf(MimeMultipart.class, msg.getContent(), "Content should be a MimeMultipart");

        MimeMultipart part = (MimeMultipart) msg.getContent();

        assertEquals(2, part.getCount(), "Should have two body items (message + attachment)");

        BodyPart attach = part.getBodyPart(1);
        assertTrue(
                "test.pdf".equalsIgnoreCase(attach.getFileName()), "There should be a PDF named \"test.pdf\" attached");
    }

    @Test
    void saveOutput() throws Exception {
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

            @Serial
            private static final long serialVersionUID = 1L;
        }
    }
}
