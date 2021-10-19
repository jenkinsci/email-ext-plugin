package hudson.plugins.emailext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.mock_javamail.Mailbox;

public class ExtendedEmailPublisherMatrixTest {

    private ExtendedEmailPublisher publisher;
    private MatrixProject project;
    private static List<DumbSlave> agents;

    @ClassRule public static JenkinsRule j = new JenkinsRule();

    @Rule public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() throws Exception {
        agents = new ArrayList<>();
        agents.add(j.createOnlineSlave(new LabelAtom("success-agent1")));
        agents.add(j.createOnlineSlave(new LabelAtom("success-agent2")));
        agents.add(j.createOnlineSlave(new LabelAtom("success-agent3")));
    }

    @Before
    public void setUp() throws Exception {
        publisher = new ExtendedEmailPublisher();
        publisher.defaultSubject = "%DEFAULT_SUBJECT";
        publisher.defaultContent = "%DEFAULT_CONTENT";
        publisher.attachBuildLog = false;

        project = j.createProject(MatrixProject.class, testName.getMethodName());
        project.getPublishersList().add(publisher);
    }

    @After
    public void tearDown() {
        Mailbox.clearAll();
    }

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
    public void testPreBuildMatrixBuildSendAgentsOnly() throws Exception{
        addAgentToProject(0,1,2);
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
    public void testPreBuildMatrixBuildSendAgentsAndParent() throws Exception {
        addAgentToProject(0,1);
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
        addAgentToProject(0,1,2);
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

    private void addAgentToProject(int ... agentInxes) throws IOException {
        AxisList list = new AxisList();
        List<String> values = new LinkedList<>();
        for (int agentInx : agentInxes) {
            values.add(agents.get(agentInx).getLabelString());
        }
        list.add(new Axis("label",values));
        project.setAxes(list);
    }
}
