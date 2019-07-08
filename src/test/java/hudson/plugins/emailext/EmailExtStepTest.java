package hudson.plugins.emailext;

import hudson.FilePath;
import hudson.model.Run;
import org.apache.commons.lang.StringEscapeUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.recipes.WithPlugin;
import org.jvnet.mock_javamail.Mailbox;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by acearl on 9/15/2015.
 */
public class EmailExtStepTest {

    @Rule
    public JenkinsRule j = new JenkinsRule()  {
        @Override
        public void before() throws Throwable {
            super.before();
            Mailbox.clearAll();
        }

        @Override
        public void after() throws Exception {
            super.after();
            Mailbox.clearAll();
        }
    };

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
        job.setDefinition(new CpsFlowDefinition("node { emailext(to: 'mickeymouse@disney.com', subject: 'Boo') }", true));
        Run<?,?> run = job.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(run);

        Mailbox mbox = Mailbox.get("mickeymouse@disney.com");
        assertEquals(1, mbox.size());
        Message msg = mbox.get(0);
        assertEquals("Boo", msg.getSubject());
    }

    @Test
    public void attachLog() throws Exception {
        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "wf");
        job.setDefinition(new CpsFlowDefinition("node { emailext(to: 'mickeymouse@disney.com', subject: 'Boo', attachLog: true) }", true));
        Run<?,?> run = job.scheduleBuild2(0).get();
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
        assertTrue("There should be a log named \"build.log\" attached", "build.log".equalsIgnoreCase(attach.getFileName()));
    }

    @Test
    public void attachment() throws Exception {
        URL url = this.getClass().getResource("/test.pdf");
        final File attachment = new File(url.getFile());

        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "wf");
        job.setDefinition(new CpsFlowDefinition("node { fileCopy('" + StringEscapeUtils.escapeJava(attachment.getAbsolutePath()) + "'); emailext (to: 'mickeymouse@disney.com', subject: 'Boo', body: 'Here is your file', attachmentsPattern: '*.pdf') }", true));
        Run<?,?> run = job.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(run);

        Mailbox mbox = Mailbox.get("mickeymouse@disney.com");
        assertEquals("Should have an email from success", 1, mbox.size());

        Message msg = mbox.get(0);
        assertTrue("Message should be multipart", msg instanceof MimeMessage);
        assertTrue("Content should be a MimeMultipart", msg.getContent() instanceof MimeMultipart);

        MimeMultipart part = (MimeMultipart)msg.getContent();

        assertEquals("Should have two body items (message + attachment)", 2, part.getCount());

        BodyPart attach = part.getBodyPart(1);
        assertTrue("There should be a PDF named \"test.pdf\" attached", "test.pdf".equalsIgnoreCase(attach.getFileName()));
    }

    @Test
    public void saveOutput() throws Exception {
        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "wf");
        job.setDefinition(new CpsFlowDefinition(
                "node {\n" +
                        "  emailext(to: 'mickeymouse@disney.com', subject: 'Boo', saveOutput: true)\n" +
                        "  archiveArtifacts '*.*'\n" +
                        "}", true));
        Run<?,?> run = job.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(run);

        Mailbox mbox = Mailbox.get("mickeymouse@disney.com");
        assertEquals(1, mbox.size());
        Message msg = mbox.get(0);
        assertEquals("Boo", msg.getSubject());
        j.assertLogContains("Archiving artifacts", run);
    }

    public static class FileCopyStep extends AbstractStepImpl {

        private final String file;

        @DataBoundConstructor
        public FileCopyStep(String file) {
            this.file = file;
        }

        public String getFile() {
            return file;
        }

        @TestExtension("attachment")
        public static class DescriptorImpl extends AbstractStepDescriptorImpl {

            public DescriptorImpl() {
                super(Execution.class);
            }

            @Override public String getFunctionName() {
                return "fileCopy";
            }

            @Override public String getDisplayName() {
                return "Copy a file into the workspace";
            }

        }

        public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Boolean> {

            @javax.inject.Inject
            private transient FileCopyStep step;
            @StepContextParameter
            private transient FilePath workspace;

            @Override protected Boolean run() throws Exception {
                FilePath file = new FilePath(new File(step.file));
                workspace.child(file.getName()).copyFrom(file);
                return true;
            }

            private static final long serialVersionUID = 1L;
        }

    }
}
