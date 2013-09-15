package hudson.plugins.emailext;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.junit.matchers.JUnitMatchers.hasItem;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.trigger.AbortedTrigger;
import hudson.plugins.emailext.plugins.trigger.FailureTrigger;
import hudson.plugins.emailext.plugins.trigger.FirstFailureTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedUnhealthyTrigger;
import hudson.plugins.emailext.plugins.trigger.NotBuiltTrigger;
import hudson.plugins.emailext.plugins.trigger.PreBuildTrigger;
import hudson.plugins.emailext.plugins.trigger.StillFailingTrigger;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import hudson.tasks.Mailer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.sf.json.JSONObject;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.mock_javamail.Mailbox;
import org.kohsuke.stapler.Stapler;

public class ExtendedEmailPublisherTest
    extends HudsonTestCase
{
    private ExtendedEmailPublisher publisher;

    private FreeStyleProject project;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        publisher = new ExtendedEmailPublisher();
        publisher.defaultSubject = "%DEFAULT_SUBJECT";
        publisher.defaultContent = "%DEFAULT_CONTENT";
        publisher.attachmentsPattern = "";
        publisher.recipientList = "%DEFAULT_RECIPIENTS";
        publisher.presendScript = "";

        project = createFreeStyleProject();
        project.getPublishersList().add( publisher );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();

        Mailbox.clearAll();
    }
    
//    public void testExcludedCommitters() 
//        throws Exception
//    {
//        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("excludedCommitters");
//        f.setAccessible(true);
//        f.set(ExtendedEmailPublisher.DESCRIPTOR, "mickeymouse@disney.com");
//        
//        
//        
//    }

    public void testShouldNotSendEmailWhenNoTriggerEnabled()
        throws Exception
    {
        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatusSuccess( build );

        List<String> log = build.getLog( 100 );
        assertThat( "No emails should have been trigger during pre-build or post-build.", log,
                    hasItems( "No emails were triggered.", "No emails were triggered." ) );
    }

    public void testPreBuildTriggerShouldAlwaysSendEmail()
        throws Exception
    {
        PreBuildTrigger trigger = new PreBuildTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatusSuccess( build );

        assertThat( "Email should have been triggered, so we should see it in the logs.", build.getLog( 100 ),
                    hasItems( "Email was triggered for: " + PreBuildTrigger.TRIGGER_NAME ) );
        assertEquals( 1, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testSuccessTriggerShouldSendEmailWhenBuildSucceeds()
        throws Exception
    {
        SuccessTrigger successTrigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( successTrigger );
        publisher.getConfiguredTriggers().add( successTrigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatusSuccess( build );

        assertThat( "Email should have been triggered, so we should see it in the logs.", build.getLog( 100 ),
                    hasItems( "Email was triggered for: Success" ) );
        assertEquals( 1, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testSuccessTriggerShouldNotSendEmailWhenBuildFails()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        SuccessTrigger trigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build );

        assertThat( "Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog( 100 ),
                    not( hasItems( "Email was triggered for: " + SuccessTrigger.TRIGGER_NAME ) ) );
        assertEquals( 0, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testFirstFailureTriggerShouldNotSendEmailOnSecondFail()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        FirstFailureTrigger trigger = new FirstFailureTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build );

        FreeStyleBuild build2 = project.scheduleBuild2( 1 ).get();
        assertBuildStatus( Result.FAILURE, build2 );

        assertThat( "Email should have been triggered for build 0, so we should see it in the logs.", build.getLog( 100 ),
                    hasItems( "Email was triggered for: " + FirstFailureTrigger.TRIGGER_NAME ) );

        assertThat( "Email should NOT have been triggered for build 1, so we shouldn't see it in the logs.", build2.getLog( 100 ),
                    not( hasItems( "Email was triggered for: " + FailureTrigger.TRIGGER_NAME ) ) );
        assertEquals( 1, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testFixedTriggerShouldNotSendEmailWhenBuildFirstFails()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        FixedTrigger trigger = new FixedTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build );

        assertThat( "Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog( 100 ),
                    not( hasItems( "Email was triggered for: " + SuccessTrigger.TRIGGER_NAME ) ) );
        assertEquals( "No email should have been sent out since the build failed only once.", 0,
                      Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testFixedTriggerShouldSendEmailWhenBuildIsFixed()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        FixedTrigger trigger = new FixedTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        FreeStyleBuild build1 = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build1 );

        project.getBuildersList().clear();
        FreeStyleBuild build2 = project.scheduleBuild2( 0 ).get();
        assertBuildStatusSuccess( build2 );

        assertThat( "Email should have been triggered, so we should see it in the logs.", build2.getLog( 100 ),
                    hasItems( "Email was triggered for: " + FixedTrigger.TRIGGER_NAME ) );
        assertEquals( 1, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testFixedTriggerShouldNotSendEmailWhenBuildSucceedsAfterAbortedBuild()
        throws Exception
    {
        // fail
        project.getBuildersList().add( new FailureBuilder() );
        FreeStyleBuild build1 = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build1 );

        // abort
        project.getBuildersList().clear();
        project.getBuildersList().add( new MockBuilder(Result.ABORTED) );
        FreeStyleBuild build2 = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.ABORTED, build2 );

        FixedTrigger trigger = new FixedTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        // succeed
        project.getBuildersList().clear();
        FreeStyleBuild build3 = project.scheduleBuild2( 0 ).get();
        assertBuildStatusSuccess( build3 );

        assertThat( "Email should not have been triggered, so we shouldn't see it in the logs.", build3.getLog( 100 ),
                    not( hasItems( "Email was triggered for: " + SuccessTrigger.TRIGGER_NAME ) ) );
        assertEquals( "No email should have been sent out since the prior build was aborted.", 0,
                      Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testFixedUnhealthyTriggerShouldNotSendEmailWhenBuildFirstFails()
            throws Exception
        {
            project.getBuildersList().add( new FailureBuilder() );

            FixedUnhealthyTrigger trigger = new FixedUnhealthyTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
                    "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
            addEmailType( trigger );
            publisher.getConfiguredTriggers().add( trigger );

            FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
            assertBuildStatus( Result.FAILURE, build );

            assertThat( "Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog( 100 ),
                        not( hasItems( "Email was triggered for: " + SuccessTrigger.TRIGGER_NAME ) ) );
            assertEquals( "No email should have been sent out since the build failed only once.", 0,
                          Mailbox.get( "ashlux@gmail.com" ).size() );
        }

        public void testFixedUnhealthyTriggerShouldSendEmailWhenBuildIsFixed()
            throws Exception
        {
            project.getBuildersList().add( new FailureBuilder() );

            FixedUnhealthyTrigger trigger = new FixedUnhealthyTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
                    "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
            addEmailType( trigger );
            publisher.getConfiguredTriggers().add( trigger );

            FreeStyleBuild build1 = project.scheduleBuild2( 0 ).get();
            assertBuildStatus( Result.FAILURE, build1 );

            project.getBuildersList().clear();
            FreeStyleBuild build2 = project.scheduleBuild2( 0 ).get();
            assertBuildStatusSuccess( build2 );

            assertThat( "Email should have been triggered, so we should see it in the logs.", build2.getLog( 100 ),
                        hasItems( "Email was triggered for: " + FixedUnhealthyTrigger.TRIGGER_NAME ) );
            assertEquals( 1, Mailbox.get( "ashlux@gmail.com" ).size() );
        }

        public void testFixedUnhealthyTriggerShouldSendEmailWhenBuildSucceedsAfterAbortedBuild()
            throws Exception
        {
            // fail
            project.getBuildersList().add( new FailureBuilder() );
            FreeStyleBuild build1 = project.scheduleBuild2( 0 ).get();
            assertBuildStatus( Result.FAILURE, build1 );

            // abort
            project.getBuildersList().clear();
            project.getBuildersList().add( new MockBuilder(Result.ABORTED) );
            FreeStyleBuild build2 = project.scheduleBuild2( 0 ).get();
            assertBuildStatus( Result.ABORTED, build2 );

            FixedUnhealthyTrigger trigger = new FixedUnhealthyTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
                    "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
            addEmailType( trigger );
            publisher.getConfiguredTriggers().add( trigger );

            // succeed
            project.getBuildersList().clear();
            FreeStyleBuild build3 = project.scheduleBuild2( 0 ).get();
            assertBuildStatusSuccess( build3 );

            assertThat( "Email should have been triggered, so we should see it in the logs.", build3.getLog( 100 ),
                    hasItems( "Email was triggered for: " + FixedUnhealthyTrigger.TRIGGER_NAME ) );
            assertEquals( 1, Mailbox.get( "ashlux@gmail.com" ).size() );
        }

    public void testStillFailingTriggerShouldNotSendEmailWhenBuildSucceeds()
        throws Exception
    {
        StillFailingTrigger trigger = new StillFailingTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatusSuccess( build );

        assertThat( "Email should not have been triggered, so we should not see it in the logs.", build.getLog( 100 ),
                    not( hasItems( "Email was triggered for: " + StillFailingTrigger.TRIGGER_NAME ) ) );
        assertEquals( 0, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testStillFailingTriggerShouldNotSendEmailWhenBuildFirstFails()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        StillFailingTrigger trigger = new StillFailingTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        // only fail once
        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build );

        assertThat( "Email should not have been triggered, so we should not see it in the logs.", build.getLog( 100 ),
                    not( hasItems( "Email was triggered for: " + StillFailingTrigger.TRIGGER_NAME ) ) );
        assertEquals( 0, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testStillFailingTriggerShouldNotSendEmailWhenBuildIsFixed()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        StillFailingTrigger trigger = new StillFailingTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        // only fail once
        FreeStyleBuild build1 = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build1 );
        // then succeed
        project.getBuildersList().clear();
        FreeStyleBuild build2 = project.scheduleBuild2( 0 ).get();
        assertBuildStatusSuccess( build2 );

        assertThat( "Email should not have been triggered, so we should not see it in the logs.", build2.getLog( 100 ),
                    not( hasItems( "Email was triggered for: " + StillFailingTrigger.TRIGGER_NAME ) ) );
        assertEquals( 0, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testStillFailingTriggerShouldSendEmailWhenBuildContinuesToFail()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        StillFailingTrigger trigger = new StillFailingTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        // first failure
        FreeStyleBuild build1 = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build1 );
        // second failure
        FreeStyleBuild build2 = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build2 );

        assertThat( "Email should have been triggered, so we should see it in the logs.", build2.getLog( 100 ),
                    hasItems( "Email was triggered for: " + StillFailingTrigger.TRIGGER_NAME ) );
        assertEquals( "We should only have one email since the first failure doesn't count as 'still failing'.", 1,
                      Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testAbortedTriggerShouldSendEmailWhenBuildAborts()
        throws Exception
    {
        project.getBuildersList().add( new MockBuilder(Result.ABORTED) );

        AbortedTrigger abortedTrigger = new AbortedTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( abortedTrigger );
        publisher.getConfiguredTriggers().add( abortedTrigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.ABORTED, build );

        assertThat( "Email should have been triggered, so we should see it in the logs.", build.getLog( 100 ),
                    hasItems( "Email was triggered for: " + AbortedTrigger.TRIGGER_NAME ) );
        assertEquals( 1, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testAbortedTriggerShouldNotSendEmailWhenBuildFails()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        AbortedTrigger trigger = new AbortedTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build );

        assertThat( "Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog( 100 ),
                    not( hasItems( "Email was triggered for: " + AbortedTrigger.TRIGGER_NAME ) ) );
        assertEquals( 0, Mailbox.get( "ashlux@gmail.com" ).size() );
    }


    public void testNotBuiltTriggerShouldSendEmailWhenNotBuilt()
        throws Exception
    {
        project.getBuildersList().add( new MockBuilder(Result.NOT_BUILT) );

        NotBuiltTrigger notbuiltTrigger = new NotBuiltTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( notbuiltTrigger );
        publisher.getConfiguredTriggers().add( notbuiltTrigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.NOT_BUILT, build );

        assertThat( "Email should have been triggered, so we should see it in the logs.", build.getLog( 100 ),
                    hasItems( "Email was triggered for: " + NotBuiltTrigger.TRIGGER_NAME ) );
        assertEquals( 1, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testNotBuiltTriggerShouldNotSendEmailWhenBuildFails()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        NotBuiltTrigger trigger = new NotBuiltTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build );

        assertThat( "Email should not have been triggered, so we shouldn't see it in the logs.", build.getLog( 100 ),
                    not( hasItems( "Email was triggered for: " + NotBuiltTrigger.TRIGGER_NAME ) ) );
        assertEquals( 0, Mailbox.get( "ashlux@gmail.com" ).size() );
    }

    public void testShouldSendEmailUsingUtf8ByDefault()
        throws Exception
    {
        project.getBuildersList().add( new FailureBuilder() );

        FailureTrigger trigger = new FailureTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();
        assertBuildStatus( Result.FAILURE, build );

        Mailbox mailbox = Mailbox.get( "ashlux@gmail.com" );
        assertEquals( "We should an email since the build failed.", 1, mailbox.size() );
        Message msg = mailbox.get(0);
        assertThat( "Message should be multipart", msg.getContentType(), 
                containsString("multipart/mixed"));
        
        // TODO: add more tests for getting the multipart information.
        if(MimeMessage.class.isInstance(msg)) {
            MimeMessage mimeMsg = (MimeMessage)msg;
            assertEquals( "Message content should be a MimeMultipart instance",
                    MimeMultipart.class, mimeMsg.getContent().getClass());
            MimeMultipart multipart = (MimeMultipart)mimeMsg.getContent();            
            assertTrue( "There should be at least one part in the email", 
                    multipart.getCount() >= 1);            
            MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(0);                     
            assertThat( "UTF-8 charset should be used.", bodyPart.getContentType(),
                    containsString( "charset=UTF-8" ) );
        } else {
            assertThat( "UTF-8 charset should be used.", mailbox.get( 0 ).getContentType(),
                        containsString( "charset=UTF-8" ) );
        }
    }
    
    public void testCancelFromPresendScriptCausesNoEmail() throws Exception {
        publisher.presendScript = "cancel = true";
        SuccessTrigger successTrigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType(){{
            setSendToRequester(true);
        }});
        publisher.getConfiguredTriggers().add( successTrigger );

        User u = User.get("kutzi");
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);
        
        UserCause cause = new MockUserCause("kutzi");
                
        FreeStyleBuild build = project.scheduleBuild2( 0, cause ).get();
        assertBuildStatusSuccess( build );

        assertEquals( 0, Mailbox.get( "kutzi@xxx.com" ).size() );
    }
    
    public void testNoCancelFromPresendScriptCausesEmail() throws Exception {
        publisher.presendScript = "def hello = 'world'\n";
        SuccessTrigger successTrigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType(){{
            setSendToRequester(true);
        }});
        publisher.getConfiguredTriggers().add( successTrigger );

        User u = User.get("kutzi");
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);
        
        UserCause cause = new MockUserCause("kutzi");
                
        FreeStyleBuild build = project.scheduleBuild2( 0, cause ).get();
        assertBuildStatusSuccess( build );

        assertEquals( 1, Mailbox.get( "kutzi@xxx.com" ).size() );
    }
    
    public void testPresendScriptModifiesTo() throws Exception {
        publisher.presendScript = "import javax.mail.Message.RecipientType\n" 
            + "msg.setRecipients(RecipientType.TO, 'slide.o.mix@xxx.com')";
        SuccessTrigger successTrigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType(){{
            setSendToRequester(true);
        }});
        publisher.getConfiguredTriggers().add( successTrigger );

        User u = User.get("kutzi");
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);
        
        UserCause cause = new MockUserCause("kutzi");
                
        FreeStyleBuild build = project.scheduleBuild2( 0, cause ).get();
        assertBuildStatusSuccess( build );

        assertEquals( 0, Mailbox.get( "kutzi@xxx.com" ).size() );
        assertEquals( 1, Mailbox.get( "slide.o.mix@xxx.com" ).size() );
    }

    public void testPresendScriptNoSecurity() throws Exception {
        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "enableSecurity" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, false );

        publisher.presendScript = "for(it in Jenkins.instance.items) {\n\tSystem.out.println(it.name)\n}\n";
        SuccessTrigger successTrigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType(){{
            setSendToRequester(true);
        }});
        publisher.getConfiguredTriggers().add( successTrigger );

        User u = User.get("kutzi");
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);
        
        UserCause cause = new MockUserCause("kutzi");
        FreeStyleBuild build = project.scheduleBuild2( 0, cause ).get();
        assertBuildStatusSuccess( build );

        assertEquals( 1, Mailbox.get( "kutzi@xxx.com" ).size() );

        assertThat( "Access was done to Jenkins instance with security enabled, so we should see an error", build.getLog( 100 ),
                   not( hasItem( "Pre-send script tried to access secured objects: Use of 'jenkins' is disallowed by security policy" ) ) );
    }

    public void testPresendScriptSecurity() throws Exception {
        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "enableSecurity" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, true );

        publisher.presendScript = "for(it in Jenkins.instance.items) {\n\tSystem.out.println(it.name)\n}\n";
        SuccessTrigger successTrigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType(){{
            setSendToRequester(true);
        }});
        publisher.getConfiguredTriggers().add( successTrigger );

        User u = User.get("kutzi");
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);
        
        UserCause cause = new MockUserCause("kutzi");
        FreeStyleBuild build = project.scheduleBuild2( 0, cause ).get();
        
        assertBuildStatusSuccess( build );

        assertEquals( 1, Mailbox.get( "kutzi@xxx.com" ).size() );

        assertThat( "Access was done to Jenkins instance with security enabled, so we should see an error", build.getLog( 100 ),
                   hasItem( "Pre-send script tried to access secured objects: Use of 'Jenkins' and 'Hudson' are disallowed by security policy" ) );
    }
    
    public void testSendToRequesterLegacy()  throws Exception {
        SuccessTrigger successTrigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType(){{
            setSendToRequester(true);
        }});
        publisher.getConfiguredTriggers().add( successTrigger );

        User u = User.get("kutzi");
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);
        
        UserCause cause = new MockUserCause("kutzi");
                
        FreeStyleBuild build = project.scheduleBuild2( 0, cause ).get();
        assertBuildStatusSuccess( build );

        assertEquals( 1, Mailbox.get( "kutzi@xxx.com" ).size() );
    }

    public void testReplyTo() throws Exception {
        SuccessTrigger successTrigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType(){{
            setSendToRequester(true);
        }});
        publisher.getConfiguredTriggers().add( successTrigger );
        publisher.replyTo = "ashlux@gmail.com";

        User u = User.get("kutzi");
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);
        
        UserCause cause = new MockUserCause("kutzi");
                
        FreeStyleBuild build = project.scheduleBuild2( 0, cause ).get();
        assertBuildStatusSuccess( build );

        Mailbox mailbox = Mailbox.get( "kutzi@xxx.com" );
        assertEquals( 1, mailbox.size() );

        Message msg = mailbox.get(0);
        Address[] replyTo = msg.getReplyTo();
        assertEquals( 1, replyTo.length );

        assertEquals( "ashlux@gmail.com", replyTo[0].toString() );
    }
    
    public void testNoReplyTo() throws Exception {
        SuccessTrigger successTrigger = new SuccessTrigger(true, true, true, false, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        successTrigger.setEmail(new EmailType(){{
            setSendToRequester(true);
        }});
        publisher.getConfiguredTriggers().add( successTrigger );
        
        User u = User.get("kutzi");
        u.setFullName("Christoph Kutzinski");
        Mailer.UserProperty prop = new Mailer.UserProperty("kutzi@xxx.com");
        u.addProperty(prop);
        
        UserCause cause = new MockUserCause("kutzi");
                
        FreeStyleBuild build = project.scheduleBuild2( 0, cause ).get();
        assertBuildStatusSuccess( build );

        Mailbox mailbox = Mailbox.get( "kutzi@xxx.com" );
        assertEquals( 1, mailbox.size() );

        Message msg = mailbox.get(0);
        Address[] replyTo = msg.getReplyTo();
        assertEquals( 1, replyTo.length );

        assertEquals( "address not configured yet <nobody@nowhere>", replyTo[0].toString() );
    }
    
    private static class MockUserCause extends UserCause {
        public MockUserCause(String userName) throws Exception {
            super();
            Field f = UserCause.class.getDeclaredField("authenticationName");
            f.setAccessible(true);
            f.set(this, userName);
        }
    }

    public void testNewInstance_shouldGetBasicInformation()
        throws Exception
    {
        createWebClient().executeOnServer(new Callable<Object>() {
            public Void call() throws Exception {
                JSONObject form = new JSONObject();
                form.put( "project_content_type", "default" );
                form.put( "project_recipient_list", "ashlux@gmail.com" );
                form.put( "project_default_subject", "Make millions in Nigeria" );
                form.put( "project_default_content", "Give me a $1000 check and I'll mail you back $5000!!!" );
                form.put( "project_attachments", "");
                form.put( "project_presend_script", "");
                form.put( "project_replyto", "");

                publisher = (ExtendedEmailPublisher) ExtendedEmailPublisher.DESCRIPTOR.newInstance(Stapler.getCurrentRequest(), form );

                assertEquals( "default", publisher.contentType );
                assertEquals( "ashlux@gmail.com", publisher.recipientList );
                assertEquals( "Make millions in Nigeria", publisher.defaultSubject );
                assertEquals( "Give me a $1000 check and I'll mail you back $5000!!!", publisher.defaultContent );
                assertEquals( "", publisher.attachmentsPattern);
                assertEquals( "", publisher.replyTo);

                return null;
            }
        });
    }

    private void addEmailType( EmailTrigger trigger )
    {
        trigger.setEmail( new EmailType()
        {{
                setRecipientList( "ashlux@gmail.com" );
                setSubject( "Yet another Hudson email" );
                setBody( "Boom goes the dynamite." );
            }} );
    }
}
