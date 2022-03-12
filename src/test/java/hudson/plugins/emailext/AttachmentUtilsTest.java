/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.mock_javamail.Mailbox;

/**
 *
 * @author acearl
 */
public class AttachmentUtilsTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @After
    public void tearDown() {
        Mailbox.clearAll();
    }

    @Test
    public void testBuildLogAttachment() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachBuildLog = true;
        publisher.recipientList = "mickey@disney.com";
        SuccessTrigger trigger = new SuccessTrigger(Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        publisher.getConfiguredTriggers().add(trigger);
        project.getPublishersList().add(publisher);
        FreeStyleBuild b = j.buildAndAssertSuccess(project);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
        assertEquals("Should have an email from success", 1, mbox.size());

        Message msg = mbox.get(0);
        assertThat(msg, instanceOf(MimeMessage.class));
        assertThat(msg.getContent(), instanceOf(MimeMultipart.class));

        MimeMultipart part = (MimeMultipart) msg.getContent();

        assertEquals("Should have two body items (message + attachment)", 2, part.getCount());

        BodyPart attach = part.getBodyPart(1);
        assertThat(attach.getSize(), greaterThan(0));
        assertThat(IOUtils.toString(attach.getInputStream(), StandardCharsets.UTF_8), containsString("mickey@disney.com"));
        assertEquals("build.log", attach.getFileName());
    }

    @Test
    public void testBuildLogZipAttachment() throws Exception {
        // check the size limit applies to the compressed size
        j.getInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class).setMaxAttachmentSize(80000);
        FreeStyleProject project = j.createFreeStyleProject("foo");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachBuildLog = true;
        publisher.compressBuildLog = true;
        publisher.recipientList = "mickey@disney.com";
        SuccessTrigger trigger = new SuccessTrigger(Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        publisher.getConfiguredTriggers().add(trigger);
        project.getPublishersList().add(publisher);
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                for (int i = 0; i < 1000; i++) // Pad out the build log so the zip has something to compress
                    listener.getLogger().println("Oh Mickey, you're so fine\n" +
                            "You're so fine you blow my mind, hey Mickey,\n" +
                            "Hey Mickey\n");
                return true;
            }
        });

        FreeStyleBuild b = j.buildAndAssertSuccess(project);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
        assertEquals("Should have an email from success", 1, mbox.size());

        Message msg = mbox.get(0);
        assertThat(msg, instanceOf(MimeMessage.class));
        assertThat(msg.getContent(), instanceOf(MimeMultipart.class));

        MimeMultipart part = (MimeMultipart) msg.getContent();

        assertEquals("Should have two body items (message + attachment)", 2, part.getCount());

        BodyPart attach = part.getBodyPart(1);
        assertThat(attach.getSize(), allOf(greaterThan(0),
                lessThanOrEqualTo((Long.valueOf(b.getLogFile().length()).intValue()))));
        assertEquals("build.zip", attach.getFileName());
        assertThat(IOUtils.toString(attach.getInputStream(), StandardCharsets.UTF_8), containsString("build.log")); // zips have plain text filename in them
    }

    @Test
    public void testAttachmentFromWorkspace() throws Exception {
        URL url = this.getClass().getResource("/test.pdf");
        final File attachment = new File(url.getFile());

        FreeStyleProject project = j.createFreeStyleProject("foo");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachmentsPattern = "*.pdf";
        publisher.recipientList = "mickey@disney.com";

        SuccessTrigger trigger = new SuccessTrigger(Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");

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

        SuccessTrigger trigger = new SuccessTrigger(Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");

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

        SuccessTrigger trigger = new SuccessTrigger(Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");

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
        assumeThat(Charset.defaultCharset(), equalTo(StandardCharsets.UTF_8));
        FreeStyleProject project = j.createFreeStyleProject("foo");

        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachmentsPattern = "**/*.txt";
        publisher.recipientList = "mickey@disney.com";

        SuccessTrigger trigger = new SuccessTrigger(Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");

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
