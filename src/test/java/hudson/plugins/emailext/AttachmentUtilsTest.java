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
import hudson.plugins.emailext.plugins.AbstractRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import org.jvnet.mock_javamail.Mailbox;
import static org.junit.Assert.*;
import org.junit.Before;
import org.jvnet.hudson.test.Issue;

/**
 *
 * @author acearl
 */
public class AttachmentUtilsTest {
    
    @Rule
    public final JenkinsRule j = new JenkinsRule();
    
    @Before
    public void setUp() {
        Mailbox.clearAll();
    }
    
    @Test
    public void testAttachmentFromWorkspace() throws Exception {
        URL url = this.getClass().getResource("/test.pdf");
        final File attachment = new File(url.getFile());
        
        FreeStyleProject project = j.createFreeStyleProject("foo");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachmentsPattern = "*.pdf";
        publisher.recipientList = "mickey@disney.com";
        
        SuccessTrigger trigger = new SuccessTrigger(Collections.<AbstractRecipientProvider>singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        
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
        
        SuccessTrigger trigger = new SuccessTrigger(Collections.<AbstractRecipientProvider>singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        
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
    public void testHtmlMimeType() throws IOException, InterruptedException, ExecutionException, Exception {
        URL url = this.getClass().getResource("/test.html");
        final File attachment = new File(url.getFile());
        
        FreeStyleProject project = j.createFreeStyleProject("foo");
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.attachmentsPattern = "**/*.html";
        publisher.recipientList = "mickey@disney.com";
        
        SuccessTrigger trigger = new SuccessTrigger(Collections.<AbstractRecipientProvider>singletonList(new ListRecipientProvider()), "", "", "", "", "", 0, "project");
        
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
}
