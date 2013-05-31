package hudson.plugins.emailext;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.labels.LabelAtom;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.trigger.PreBuildTrigger;
import hudson.slaves.DumbSlave;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.mock_javamail.Mailbox;

public class ExtendedEmailPublisherMatrixTest extends HudsonTestCase {

    private ExtendedEmailPublisher publisher;
    private MatrixProject project;
    private List<DumbSlave> slaves;	
 
    public void setUp() throws Exception{
        super.setUp();

        publisher = new ExtendedEmailPublisher();
        publisher.defaultSubject = "%DEFAULT_SUBJECT";
        publisher.defaultContent = "%DEFAULT_CONTENT";

        project = createMatrixProject();
        project.getPublishersList().add( publisher );
        slaves = new ArrayList<DumbSlave>(); 
        slaves.add(createOnlineSlave(new LabelAtom("success-slave1")));
        slaves.add(createOnlineSlave(new LabelAtom("success-slave2")));
        slaves.add(createOnlineSlave(new LabelAtom("success-slave3"))); 
    }

    public void tearDown() throws Exception {
        super.tearDown();
        slaves.clear();
        Mailbox.clearAll();
    }

    public void testPreBuildMatrixBuildSendParentOnly() throws Exception {
        publisher.setMatrixTriggerMode(MatrixTriggerMode.ONLY_PARENT);
        PreBuildTrigger trigger = new PreBuildTrigger(true, true, true, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0);
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );
        MatrixBuild build = project.scheduleBuild2(0).get();
        assertBuildStatusSuccess(build);
    
    
        assertThat( "Email should have been triggered, so we should see it in the logs.", build.getLog( 100 ),
                hasItems( "Email was triggered for: " + PreBuildTrigger.TRIGGER_NAME ) );
        assertEquals( 1, Mailbox.get( "solganik@gmail.com" ).size() );
    }

    public void testPreBuildMatrixBuildSendSlavesOnly() throws Exception{	
        addSlaveToProject(0,1,2);
    
        publisher.setMatrixTriggerMode(MatrixTriggerMode.ONLY_CONFIGURATIONS);
        PreBuildTrigger trigger = new PreBuildTrigger(true, true, true, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0);
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );
       
    
        MatrixBuild build = project.scheduleBuild2(0).get();
        assertBuildStatusSuccess(build);		
        assertEquals( 3, Mailbox.get( "solganik@gmail.com" ).size() );	
    }

    public void testPreBuildMatrixBuildSendSlavesAndParent() throws Exception{	
        addSlaveToProject(0,1);
    
        publisher.setMatrixTriggerMode(MatrixTriggerMode.BOTH);
        PreBuildTrigger trigger = new PreBuildTrigger(true, true, true, "$DEFAULT_RECIPIENTS",
            "$DEFAULT_REPLYTO", "$DEFAULT_SUBJECT", "$DEFAULT_CONTENT", "", 0);
        addEmailType( trigger );
        publisher.getConfiguredTriggers().add( trigger );
       
    
        MatrixBuild build = project.scheduleBuild2(0).get();
        assertBuildStatusSuccess(build);		
        assertEquals( 3, Mailbox.get( "solganik@gmail.com" ).size() );	
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
