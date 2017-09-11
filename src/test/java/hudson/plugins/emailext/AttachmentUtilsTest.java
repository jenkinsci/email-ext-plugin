/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.mock_javamail.Mailbox;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author acearl
 */
public class AttachmentUtilsTest {

    @Rule
    public final JenkinsRule j = new JenkinsRule() {
        @Override
        public void before() throws Throwable {
            Mailbox.clearAll();
            super.before();
        }
    };

    @Test
    public void testAttachmentFromWorkspace() throws Exception {
        URL url = this.getClass().getResource("/test.pdf");
        final File attachment = new File(url.getFile());

        FreeStyleProject project = j.createFreeStyleProject("foo");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachmentsPattern = "*.pdf";
        publisher.recipientList = "mickey@disney.com";

        SuccessTrigger trigger = new SuccessTrigger(Collections.<RecipientProvider>singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");

        publisher.getConfiguredTriggers().add(trigger);

        project.getPublishersList().add(publisher);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("test.pdf").copyFrom(new FilePath(attachment));
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
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
    public void testAttachmentFromWorkspaceSubdir() throws Exception {
        URL url = this.getClass().getResource("/test.pdf");
        final File attachment = new File(url.getFile());

        FreeStyleProject project = j.createFreeStyleProject("foo");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachmentsPattern = "**/*.pdf";
        publisher.recipientList = "mickey@disney.com";

        SuccessTrigger trigger = new SuccessTrigger(Collections.<RecipientProvider>singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");

        publisher.getConfiguredTriggers().add(trigger);

        project.getPublishersList().add(publisher);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("testreport").mkdirs();
                build.getWorkspace().child("testreport").child("test.pdf").copyFrom(new FilePath(attachment));
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
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
    @Issue("JENKINS-27062")
    public void testHtmlMimeType() throws Exception {
        URL url = this.getClass().getResource("/test.html");
        final File attachment = new File(url.getFile());

        FreeStyleProject project = j.createFreeStyleProject("foo");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachmentsPattern = "**/*.html";
        publisher.recipientList = "mickey@disney.com";

        SuccessTrigger trigger = new SuccessTrigger(Collections.<RecipientProvider>singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");

        publisher.getConfiguredTriggers().add(trigger);

        project.getPublishersList().add(publisher);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("testreport").mkdirs();
                build.getWorkspace().child("testreport").child("test.html").copyFrom(new FilePath(attachment));
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(b);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
        assertEquals("Should have an email from success", 1, mbox.size());

        Message msg = mbox.get(0);
        assertTrue("Message should be multipart", msg instanceof MimeMessage);
        assertTrue("Content should be a MimeMultipart", msg.getContent() instanceof MimeMultipart);

        MimeMultipart part = (MimeMultipart)msg.getContent();

        assertEquals("Should have two body items (message + attachment)", 2, part.getCount());

        BodyPart attach = part.getBodyPart(1);
        assertTrue("There should be a HTML file named \"test.html\" attached", "test.html".equalsIgnoreCase(attach.getFileName()));

        assertTrue("The file should have the \"text/html\" mimetype", attach.isMimeType("text/html"));
    }

    @Test
    @Issue("JENKINS-33574")
    public void testNonEnglishCharacter() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");

        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachmentsPattern = "**/*.txt";
        publisher.recipientList = "mickey@disney.com";

        SuccessTrigger trigger = new SuccessTrigger(Collections.<RecipientProvider>singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");

        publisher.getConfiguredTriggers().add(trigger);

        project.getPublishersList().add(publisher);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("已使用红包.txt").write("test.property=success","UTF-8");
                return build.getWorkspace().child("已使用红包.txt").exists();
            }
        });

        FreeStyleBuild b = project.scheduleBuild2(0).get();

        j.assertBuildStatusSuccess(b);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
        assertEquals("Should have an email from success", 1, mbox.size());

        Message msg = mbox.get(0);
        assertTrue("Message should be multipart", msg instanceof MimeMessage);
        assertTrue("Content should be a MimeMultipart", msg.getContent() instanceof MimeMultipart);

        MimeMultipart part = (MimeMultipart)msg.getContent();

        assertEquals("Should have two body items (message + attachment)", 2, part.getCount());

        BodyPart attach = part.getBodyPart(1);
        String attachment_filename = MimeUtility.decodeText(attach.getFileName());
        assertTrue(String.format("There should be a txt file named \"已使用红包.txt\" attached but found %s", attachment_filename),
                   "已使用红包.txt".equalsIgnoreCase(attachment_filename));

        assertTrue("The file should have the \"text/plain\" mimetype", attach.isMimeType("text/plain"));
    }
}
