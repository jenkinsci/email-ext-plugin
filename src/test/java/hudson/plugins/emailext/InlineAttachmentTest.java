package hudson.plugins.emailext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.mock_javamail.Mailbox;

@WithJenkins
class InlineAttachmentTest {

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
    void testSimpleInlineAttachment() throws Exception {
        URL url = this.getClass().getResource("/blank.gif");
        final File attachment = new File(url.getFile());

        FreeStyleProject project = j.createFreeStyleProject("foo");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.recipientList = "mickey@disney.com";
        publisher.contentType = "text/html";
        publisher.defaultContent = "<html><body><img src=\"cid:blank.gif\"></body></html>";
        publisher.inlineAttachmentsPattern = "blank.gif";

        SuccessTrigger trigger = new SuccessTrigger(
                Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        publisher.getConfiguredTriggers().add(trigger);
        project.getPublishersList().add(publisher);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("blank.gif").copyFrom(new FilePath(attachment));
                return true;
            }
        });

        FreeStyleBuild b = j.buildAndAssertSuccess(project);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
        assertEquals(1, mbox.size());

        Message msg = mbox.get(0);
        assertThat(msg, instanceOf(MimeMessage.class));
        
        // Structure: mixed -> related -> (html, inline)
        assertTrue(msg.getContentType().contains("multipart/mixed"));
        MimeMultipart mixed = (MimeMultipart) msg.getContent();
        assertEquals(1, mixed.getCount()); // Just the content part, no regular attachments

        BodyPart contentPart = mixed.getBodyPart(0);
        assertTrue(contentPart.getContentType().contains("multipart/related"));
        MimeMultipart related = (MimeMultipart) contentPart.getContent();
        assertEquals(2, related.getCount()); // HTML + 1 inline attachment

        BodyPart htmlPart = related.getBodyPart(0);
        assertTrue(htmlPart.getContentType().contains("text/html"));

        BodyPart inlinePart = related.getBodyPart(1);
        assertEquals("<blank.gif>", inlinePart.getHeader("Content-ID")[0]);
        assertEquals("inline", inlinePart.getDisposition());
    }

    @Test
    void testMultipleInlineAttachments() throws Exception {
        URL url1 = this.getClass().getResource("/blank.gif");
        URL url2 = this.getClass().getResource("/test.pdf");
        final File attachment1 = new File(url1.getFile());
        final File attachment2 = new File(url2.getFile());

        FreeStyleProject project = j.createFreeStyleProject("bar");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.recipientList = "mickey@disney.com";
        publisher.contentType = "text/html";
        publisher.defaultContent = "<html><body><img src=\"cid:blank.gif\"><a href=\"cid:test.pdf\">link</a></body></html>";
        publisher.inlineAttachmentsPattern = "blank.gif, test.pdf";

        SuccessTrigger trigger = new SuccessTrigger(
                Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        publisher.getConfiguredTriggers().add(trigger);
        project.getPublishersList().add(publisher);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("blank.gif").copyFrom(new FilePath(attachment1));
                build.getWorkspace().child("test.pdf").copyFrom(new FilePath(attachment2));
                return true;
            }
        });

        j.buildAndAssertSuccess(project);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
        assertEquals(1, mbox.size());

        MimeMultipart mixed = (MimeMultipart) mbox.get(0).getContent();
        MimeMultipart related = (MimeMultipart) mixed.getBodyPart(0).getContent();
        
        assertEquals(3, related.getCount()); // HTML + 2 inline attachments
        
        // Verify both inline attachments have correct Content-IDs
        String cid1 = related.getBodyPart(1).getHeader("Content-ID")[0];
        String cid2 = related.getBodyPart(2).getHeader("Content-ID")[0];
        
        assertTrue(cid1.equals("<blank.gif>") || cid1.equals("<test.pdf>"));
        assertTrue(cid2.equals("<blank.gif>") || cid2.equals("<test.pdf>"));
    }

    @Test
    void testInlineAndRegularAttachmentsCombined() throws Exception {
        URL url1 = this.getClass().getResource("/blank.gif");
        URL url2 = this.getClass().getResource("/test.pdf");
        final File attachment1 = new File(url1.getFile());
        final File attachment2 = new File(url2.getFile());

        FreeStyleProject project = j.createFreeStyleProject("combined");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.recipientList = "mickey@disney.com";
        publisher.contentType = "text/html";
        publisher.defaultContent = "<html><body><img src=\"cid:blank.gif\"></body></html>";
        publisher.inlineAttachmentsPattern = "blank.gif";
        publisher.attachmentsPattern = "test.pdf";

        SuccessTrigger trigger = new SuccessTrigger(
                Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        publisher.getConfiguredTriggers().add(trigger);
        project.getPublishersList().add(publisher);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("blank.gif").copyFrom(new FilePath(attachment1));
                build.getWorkspace().child("test.pdf").copyFrom(new FilePath(attachment2));
                return true;
            }
        });

        j.buildAndAssertSuccess(project);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
        MimeMultipart mixed = (MimeMultipart) mbox.get(0).getContent();
        
        // Structure: mixed -> [related wrapper, test.pdf]
        assertEquals(2, mixed.getCount()); 
        assertEquals("test.pdf", mixed.getBodyPart(1).getFileName());
        
        MimeMultipart related = (MimeMultipart) mixed.getBodyPart(0).getContent();
        assertEquals(2, related.getCount()); // HTML + blank.gif
        assertEquals("<blank.gif>", related.getBodyPart(1).getHeader("Content-ID")[0]);
    }

    @Test
    void testTriggerInlineAttachment() throws Exception {
        URL url = this.getClass().getResource("/blank.gif");
        final File attachment = new File(url.getFile());

        FreeStyleProject project = j.createFreeStyleProject("trigger_level");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.recipientList = "mickey@disney.com";
        // Project level has no inline, but trigger level will
        
        SuccessTrigger trigger = new SuccessTrigger(
                Collections.singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        trigger.getEmail().setBody("<html><body><img src=\"cid:blank.gif\"></body></html>");
        trigger.getEmail().setContentType("text/html");
        trigger.getEmail().setInlineAttachmentsPattern("blank.gif");
        
        publisher.getConfiguredTriggers().add(trigger);
        project.getPublishersList().add(publisher);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("blank.gif").copyFrom(new FilePath(attachment));
                return true;
            }
        });

        j.buildAndAssertSuccess(project);

        Mailbox mbox = Mailbox.get("mickey@disney.com");
        assertEquals(1, mbox.size());

        MimeMultipart mixed = (MimeMultipart) mbox.get(0).getContent();
        MimeMultipart related = (MimeMultipart) mixed.getBodyPart(0).getContent();
        
        assertEquals(2, related.getCount()); 
        assertEquals("<blank.gif>", related.getBodyPart(1).getHeader("Content-ID")[0]);
    }
}
