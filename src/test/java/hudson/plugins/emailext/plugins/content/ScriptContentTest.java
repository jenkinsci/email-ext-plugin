package hudson.plugins.emailext.plugins.content;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import hudson.util.DescribableList;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import javax.mail.Message;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.recipes.LocalData;
import org.jvnet.mock_javamail.Mailbox;

public class ScriptContentTest {
    private ScriptContent scriptContent;

    private ExtendedEmailPublisher publisher;
    
    private AbstractBuild build;
    
    private TaskListener listener;

    @Rule
    public JenkinsRule j = new JenkinsRule() {
        @Override
        public void before() throws Throwable {
            super.before();

            Mailbox.clearAll();
            
            scriptContent = new ScriptContent();
            listener = StreamTaskListener.fromStdout();

            JenkinsLocationConfiguration.get().setUrl("http://localhost");

            publisher = new ExtendedEmailPublisher();
            publisher.defaultContent = "For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!";
            publisher.defaultSubject = "How would you like your very own AWESOME-O 4000?";
            publisher.recipientList = "ashlux@gmail.com";

            Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "defaultBody" );
            f.setAccessible( true );
            f.set( publisher.getDescriptor(), "Give me $4000 and I'll mail you a check for $40,000!" );
            f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "defaultSubject" );
            f.setAccessible( true );
            f.set( publisher.getDescriptor(), "Nigerian needs your help!" );

            f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "recipientList" );
            f.setAccessible( true );
            f.set( publisher.getDescriptor(), "ashlux@gmail.com" );

            build =  mock(AbstractBuild.class);
            AbstractProject<?, ?> project = mock(AbstractProject.class);
            DescribableList publishers = mock(DescribableList.class);
            when(publishers.get(ExtendedEmailPublisher.class)).thenReturn(publisher);
            when(project.getPublishersList()).thenReturn(publishers);
            when(build.getProject()).thenReturn(project);
        }
    };
    
    @Test
    public void testShouldFindScriptOnClassPath()
            throws Exception
    {
        scriptContent.script = "empty-script-on-classpath.groovy";
        assertEquals("HELLO WORLD!", scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
    }

    @Test
    public void testShouldFindTemplateOnClassPath()
        throws Exception
    {
        scriptContent.template = "empty-groovy-template-on-classpath.template";
        // the template adds a newline
        assertEquals("HELLO WORLD!\n", scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
    }

    @Test
    @LocalData
    public void testTemplateShouldBeLoadedFromTheClosestExistingFolderConfigInTheHierarchyUpToGlobalConfig()
        throws Exception
    {
        // create project and launch a job execution just to pass the contextual build object
        // (knowing location within folder/job/item tree) to the content processing method
        FreeStyleProject globalJob = j.jenkins.createProject(FreeStyleProject.class, "test-job");
        Run<?,?> globalRun = globalJob.scheduleBuild2(0).get();
  
        scriptContent.template = "managed:email-ext-template-defined-at-global-and-parent-folder-and-test-folders-levels.template";
        assertEquals("HELLO WORLD from global config (template defined at global and parent folder and test folder levels)!",
            scriptContent.evaluate((AbstractBuild<?, ?>) globalRun, listener, ScriptContent.MACRO_NAME));
        scriptContent.template = "managed:email-ext-template-defined-at-global-and-parent-folder-levels-but-not-test-folder-level.template";
        assertEquals("HELLO WORLD from global config (template defined at global and parent folder levels but not test folder level)!",
            scriptContent.evaluate((AbstractBuild<?, ?>) globalRun, listener, ScriptContent.MACRO_NAME));

        // create project and launch a job execution just to pass the contextual build object
        // (knowing location within folder/job/item tree) to the content processing method
        FreeStyleProject parentFolderJob = ((Folder) j.jenkins.getItemByFullName("parent-folder"))
            .createProject(FreeStyleProject.class, "test-job");
        Run<?,?> parentFolderRun = parentFolderJob.scheduleBuild2(0).get();

        scriptContent.template = "managed:email-ext-template-defined-at-global-and-parent-folder-and-test-folders-levels.template";
        assertEquals("HELLO WORLD from parent-folder config (template defined at global and parent folder and test folder levels)!",
            scriptContent.evaluate((AbstractBuild<?, ?>) parentFolderRun, listener, ScriptContent.MACRO_NAME));
        scriptContent.template = "managed:email-ext-template-defined-at-global-and-parent-folder-levels-but-not-test-folder-level.template";
        assertEquals("HELLO WORLD from parent-folder config (template defined at global and parent folder levels but not test folder level)!",
            scriptContent.evaluate((AbstractBuild<?, ?>) parentFolderRun, listener, ScriptContent.MACRO_NAME));

        // create project and launch a job execution just to pass the contextual build object
        // (knowing location within folder/job/item tree) to the content processing method
        FreeStyleProject testFolderJob = ((Folder) ((Folder) j.jenkins.getItemByFullName("parent-folder")).getItem("test-folder"))
            .createProject(FreeStyleProject.class, "test-job");
        Run<?,?> testFolderRun = testFolderJob.scheduleBuild2(0).get();

        scriptContent.template = "managed:email-ext-template-defined-at-global-and-parent-folder-and-test-folders-levels.template";
        assertEquals("HELLO WORLD from test-folder config (template defined at global and parent folder and test folder levels)!",
            scriptContent.evaluate((AbstractBuild<?, ?>) testFolderRun, listener, ScriptContent.MACRO_NAME));
        scriptContent.template = "managed:email-ext-template-defined-at-global-and-parent-folder-levels-but-not-test-folder-level.template";
        assertEquals("HELLO WORLD from parent-folder config (template defined at global and parent folder levels but not test folder level)!",
            scriptContent.evaluate((AbstractBuild<?, ?>) testFolderRun, listener, ScriptContent.MACRO_NAME));
    }

    @Test
    public void testWhenScriptNotFoundThrowFileNotFoundException()
            throws Exception
    {
        scriptContent.script = "script-does-not-exist";
        assertEquals("Groovy Script file [script-does-not-exist] was not found in $JENKINS_HOME/email-templates.", 
            scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
    }

    @Test
    public void testWhenTemplateNotFoundThrowFileNotFoundException()
            throws Exception
    {
        scriptContent.template = "template-does-not-exist";
        assertEquals("Groovy Template file [template-does-not-exist] was not found in $JENKINS_HOME/email-templates.", 
            scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
    }

    @Test
    public void testWhenScriptOutsideScriptsFolderThrowFileNotFoundException()
            throws Exception
    {
        File f = File.createTempFile("does-exist-but-wrong-place", ".groovy");
        assertTrue(f.exists());
        scriptContent.script = f.getAbsolutePath();
        assertThat(scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME),
                stringContainsInOrder(Arrays.asList("Groovy Script file [", "] was not found in $JENKINS_HOME/email-templates.")));
    }

    @Test
    public void testWhenTemplateOutsideScriptsFolderThrowFileNotFoundException()
            throws Exception
    {
        File f = File.createTempFile("does-exist-but-wrong-place", ".jelly");
        assertTrue(f.exists());
        scriptContent.template = f.getAbsolutePath();
        assertThat(scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME),
                stringContainsInOrder(Arrays.asList("Groovy Template file [", "] was not found in $JENKINS_HOME/email-templates.")));
    }
    
    @Test
    public void testGroovyTemplateWithContentToken()
            throws Exception
    {
        EnvVars env = new EnvVars();
        env.put("BUILD_ID", "34");

        scriptContent.template = "content-token.template";
        
        // mock the build 
        when(build.getResult()).thenReturn(Result.SUCCESS);
        when(build.getUrl()).thenReturn("email-test/34");
        when(build.getId()).thenReturn("34");
        when(build.getEnvironment(any(TaskListener.class))).thenReturn(env);
        
        // mock changeSet
        mockChangeSet(build);
        
        // generate result from groovy template
        String content = scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME);

        // read expected file in resource to easy compare
        String expectedFile = "hudson/plugins/emailext/templates/" + "content-token.result";
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(expectedFile);
        String expected = new Scanner(in).useDelimiter("\\Z").next();
        
        // windows has a \r in each line, so make sure the comparison works correctly
        if(Functions.isWindows()) { 
            expected = expected.replace("\r", "");
        }
        
        // remove end space before compare
        assertEquals(expected.trim(), content.trim());
    }
    
    /**
     * this is for groovy template testing 
     */
    @Test
    public void testWithGroovyTemplate() throws Exception {
        scriptContent.template = "groovy-sample.template";
        EnvVars env = new EnvVars();
        env.put("BUILD_ID", "34");

        // mock the build 
        when(build.getResult()).thenReturn(Result.SUCCESS);
        when(build.getUrl()).thenReturn("email-test/34");
        when(build.getId()).thenReturn("34");
        when(build.getEnvironment(any(TaskListener.class))).thenReturn(env);
        
        // mock changeSet
        mockChangeSet(build);
        
        // generate result from groovy template
        String content = scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME);

        // read expected file in resource to easy compare
        String expectedFile = "hudson/plugins/emailext/templates/" + "groovy-sample.result";
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(expectedFile);
        String expected = new Scanner(in).useDelimiter("\\Z").next();
        
        // windows has a \r in each line, so make sure the comparison works correctly
        if(Functions.isWindows()) { 
            expected = expected.replace("\r", "");
        }
        // remove end space before compare
        assertEquals(expected.trim(), content.trim());
    }

    @Test public void templateOnDisk() throws Exception {
        scriptContent.template = "testing1.template";
        FileUtils.write(new File(ScriptContent.scriptsFolder(), "testing1.template"), "2+2=${2+2}", StandardCharsets.UTF_8);
        assertEquals("2+2=4", scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            assertEquals("2+2=4", scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
        }
        long end = System.currentTimeMillis();
        System.out.printf("average time %.2fmsec%n", (end - start) / 1000.0);
        FileUtils.write(new File(ScriptContent.scriptsFolder(), "testing1.template"), "2 + 2 = ${2+2}", StandardCharsets.UTF_8);
        assertEquals("2 + 2 = 4", scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
        scriptContent.template = "testing2.template";
        FileUtils.write(new File(ScriptContent.scriptsFolder(), "testing2.template"), "2 + 2 is ${2+2}", StandardCharsets.UTF_8);
        assertEquals("2 + 2 is 4", scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
        scriptContent.template = "testing1.template";
        assertEquals("2 + 2 = 4", scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
    }

    @Test public void templateInWorkspace() throws Exception {
        templateInWorkspace("/test.groovy");
    }

    @Test public void templateInWorkspaceUnsafe() throws Exception {
        templateInWorkspace("/testUnsafe.groovy");
    }
    
    public void templateInWorkspace(String scriptResource) throws Exception {
        URL url = this.getClass().getResource(scriptResource);
        final File script = new File(url.getFile());
        
        FreeStyleProject p = j.createFreeStyleProject("foo");
        
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.recipientList = "mickey@disney.com";
        publisher.defaultSubject = "${SCRIPT, script=\"subdir/test.groovy\"}";
        
        SuccessTrigger trigger = new SuccessTrigger(Collections.singletonList(new ListRecipientProvider()), "", "", "$PROJECT_DEFAULT_SUBJECT", "", "", 0, "project");
        
        publisher.getConfiguredTriggers().add(trigger);
        
        p.getPublishersList().add(publisher);
        
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                FilePath p = build.getWorkspace().child("subdir");
                p.mkdirs();
                p = p.child("test.groovy");
                p.copyFrom(new FilePath(script));
                return p.exists();
            }
        });
        FreeStyleBuild b = p.scheduleBuild2(0).get();     
        j.assertBuildStatusSuccess(b);
        
        Mailbox mbox = Mailbox.get("mickey@disney.com");
        assertEquals("Should have an email from success", 1, mbox.size());
        
        Message msg = mbox.get(0);
        assertEquals("foo[3] = 4", msg.getSubject());
    }

    private void mockChangeSet(final AbstractBuild build) {
        ScriptContentChangeLogSet changeLog = new ScriptContentChangeLogSet(build);
        when(build.getChangeSet()).thenReturn(changeLog);
        when(build.getChangeSets()).thenReturn(Collections.singletonList(changeLog));
    }
}
