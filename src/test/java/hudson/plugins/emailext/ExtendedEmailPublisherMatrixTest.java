package hudson.plugins.emailext;

import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.labels.LabelAtom;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.trigger.AlwaysTrigger;
import hudson.plugins.emailext.plugins.trigger.PreBuildTrigger;
import hudson.slaves.DumbSlave;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.mock_javamail.Mailbox;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class ExtendedEmailPublisherMatrixTest {

    private ExtendedEmailPublisher publisher;
    private MatrixProject project;
    private List<DumbSlave> slaves;    
 
    @Rule
    public JenkinsRule j = new JenkinsRule() { 
        @Override
        public void before() throws Throwable {
            super.before();

            publisher = new ExtendedEmailPublisher();
            publisher.defaultSubject = "%DEFAULT_SUBJECT";
            publisher.defaultContent = "%DEFAULT_CONTENT";
            publisher.attachBuildLog = false;

            project = j.jenkins.createProject(MatrixProject.class, "Foo");
            project.getPublishersList().add( publisher );
            slaves = new ArrayList<DumbSlave>(); 
            slaves.add(createOnlineSlave(new LabelAtom("success-slave1")));
            slaves.add(createOnlineSlave(new LabelAtom("success-slave2")));
            slaves.add(createOnlineSlave(new LabelAtom("success-slave3"))); 
        }
        
        @Override
        public void after() throws Exception {
            super.after();
            slaves.clear();
            Mailbox.clearAll();
        }
    };

    @Test
    public void testPreBuildMatrixBuildSendParentOnly() throws Exception {
        publisher.setMatrixTriggerMode(MatrixTriggerMode.ONLY_PARENT);
        List<RecipientProvider> recProviders = Collections.emptyList();
        PreBuildTrigger trigger = new PreBuildTrigger(recProviders, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );
        MatrixBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);    
    
        assertThat( "Email should have been triggered, so we should see it in the logs.", build.getLog( 100 ),
                hasItems( "Email was triggered for: " + PreBuildTrigger.TRIGGER_NAME ) );
        assertEquals( 1, Mailbox.get( "solganik@gmail.com" ).size() );
    }

    @Test
    public void testPreBuildMatrixBuildSendSlavesOnly() throws Exception{    
        addSlaveToProject(0,1,2);
        List<RecipientProvider> recProviders = Collections.emptyList();
        publisher.setMatrixTriggerMode(MatrixTriggerMode.ONLY_CONFIGURATIONS);
        PreBuildTrigger trigger = new PreBuildTrigger(recProviders, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );
       
    
        MatrixBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);        
        assertEquals( 3, Mailbox.get( "solganik@gmail.com" ).size() );    
    }

    @Test
    public void testPreBuildMatrixBuildSendSlavesAndParent() throws Exception {    
        addSlaveToProject(0,1);
        List<RecipientProvider> recProviders = Collections.emptyList();
        publisher.setMatrixTriggerMode(MatrixTriggerMode.BOTH);
        PreBuildTrigger trigger = new PreBuildTrigger(recProviders, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );
       
    
        MatrixBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);        
        assertEquals( 3, Mailbox.get( "solganik@gmail.com" ).size() );    
    }
    
    @Test
    public void testAttachBuildLogForAllAxes() throws Exception { 
        publisher.setMatrixTriggerMode(MatrixTriggerMode.ONLY_PARENT);
        publisher.attachBuildLog = true;
        addSlaveToProject(0,1,2);
        List<RecipientProvider> recProviders = Collections.emptyList();
        AlwaysTrigger trigger = new AlwaysTrigger(recProviders, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0, "project");
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );
        MatrixBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);    
    
        assertThat( "Email should have been triggered, so we should see it in the logs.", build.getLog( 100 ),
                hasItems( "Email was triggered for: " + AlwaysTrigger.TRIGGER_NAME ) );
        
        assertEquals( 1, Mailbox.get( "solganik@gmail.com" ).size() );
        
        Message msg = Mailbox.get("solganik@gmail.com").get(0);
        
        assertTrue("Message should be multipart", msg instanceof MimeMessage);
        assertTrue("Content should be a MimeMultipart", msg.getContent() instanceof MimeMultipart);
        
        MimeMultipart part = (MimeMultipart)msg.getContent();
        
        assertEquals("Should have four body items (message + attachment)", 4, part.getCount());
        
        int i = 1;
        for(MatrixRun r : build.getExactRuns()) {
            String fileName = "build" + "-" + r.getParent().getCombination().toString('-', '-') +  ".log";
            BodyPart attach = part.getBodyPart(i);
            assertTrue("There should be a log named \"" + fileName + "\" attached", fileName.equalsIgnoreCase(attach.getFileName()));        
            i++;
        }
    }

    private void addEmailType( EmailTrigger trigger ) {
        trigger.setEmail( new EmailType()
        {{
            setRecipientList( "solganik@gmail.com" );
            setSubject( "Yet another Hudson email" );
            setBody( "Boom goes the dynamite." );
        }} );
    }

    private void addSlaveToProject(int ... slaveInxes ) throws IOException {
        AxisList list = new AxisList();
        List<String> values = new LinkedList<String>();
        for (int slaveInx : slaveInxes) {
            values.add(slaves.get(slaveInx).getLabelString());
        }
        list.add(new Axis("label",values));
        project.setAxes(list);
    }
}
