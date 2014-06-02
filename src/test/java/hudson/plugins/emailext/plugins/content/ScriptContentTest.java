package hudson.plugins.emailext.plugins.content;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import hudson.util.DescribableList;
import hudson.util.StreamTaskListener;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;
import jenkins.model.JenkinsLocationConfiguration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

public class ScriptContentTest {
    private ScriptContent scriptContent;

    private final String osName = System.getProperty("os.name");

    private final boolean osIsDarwin = osName.equals("Mac OS X") || osName.equals("Darwin");

    private ExtendedEmailPublisher publisher;
    
    private AbstractBuild build;
    
    private TaskListener listener;

    @Rule
    public JenkinsRule rule = new JenkinsRule() {
        @Override
        public void before() throws Throwable {
            assumeThat(osIsDarwin, is(false));
            super.before();
        }
    };
    

    @Before
    public void setup() throws Throwable
    {
        assumeThat(osIsDarwin, is(false));
        
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
        AbstractProject project = mock(AbstractProject.class);
        DescribableList publishers = mock(DescribableList.class);
        when(publishers.get(ExtendedEmailPublisher.class)).thenReturn(publisher);
        when(project.getPublishersList()).thenReturn(publishers);
        when(build.getProject()).thenReturn(project);
    }


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
    public void testWhenScriptNotFoundThrowFileNotFoundException()
            throws Exception
    {
        scriptContent.script = "script-does-not-exist";
        assertEquals("Script [script-does-not-exist] was not found in $JENKINS_HOME/email-templates.", 
            scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
    }

    @Test
    public void testWhenTemplateNotFoundThrowFileNotFoundException()
            throws Exception
    {
        scriptContent.template = "template-does-not-exist";
        assertEquals("Template [template-does-not-exist] was not found in $JENKINS_HOME/email-templates.", 
            scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME));
    }
    
    @Test
    public void testGroovyTemplateWithContentToken()
            throws Exception
    {
        scriptContent.template = "content-token.template";
        
        // mock the build 
        when(build.getResult()).thenReturn(Result.SUCCESS);
        when(build.getUrl()).thenReturn("email-test/34");
        when(build.getId()).thenReturn("34");
        
        // mock changeSet
        mockChangeSet(build);
        
        // generate result from groovy template
        String content = scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME);

        // read expected file in resource to easy compare
        String expectedFile = "hudson/plugins/emailext/templates/" + "content-token.result";
        InputStream in = getClass().getClassLoader().getResourceAsStream(expectedFile);
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
     * @throws Exception
     */
    @Test
    public void testWithGroovyTemplate() throws Exception {
        scriptContent.template = "groovy-sample.template";

        // mock the build 
        when(build.getResult()).thenReturn(Result.SUCCESS);
        when(build.getUrl()).thenReturn("email-test/34");
        when(build.getId()).thenReturn("34");
        
        // mock changeSet
        mockChangeSet(build);
        
        // generate result from groovy template
        String content = scriptContent.evaluate(build, listener, ScriptContent.MACRO_NAME);

        // read expected file in resource to easy compare
        String expectedFile = "hudson/plugins/emailext/templates/" + "groovy-sample.result";
        InputStream in = getClass().getClassLoader().getResourceAsStream(expectedFile);
        String expected = new Scanner(in).useDelimiter("\\Z").next();
        
        // windows has a \r in each line, so make sure the comparison works correctly
        if(Functions.isWindows()) { 
            expected = expected.replace("\r", "");
        }
        // remove end space before compare
        assertEquals(expected.trim(), content.trim());
    }
    
    private void mockChangeSet(final AbstractBuild build) {
        Mockito.when(build.getChangeSet()).thenReturn(new ChangeLogSet(build) {
            @Override
            public boolean isEmptySet() {
                return false;
            }

            public Iterator iterator() {
                return Arrays.asList(new Entry() {
                    @Override
                    public String getMsg() {
                        return "COMMIT MESSAGE";
                    }

                    @Override
                    public User getAuthor() {
                        User user = mock(User.class);
                        when(user.getDisplayName()).thenReturn("Kohsuke Kawaguchi");
                        when(user.getFullName()).thenReturn("Kohsuke Kawaguchi");
                        return user;
                    }

                    @Override
                    public Collection<String> getAffectedPaths() {
                        return Arrays.asList("path1", "path2");
                    }

                    @Override
                    public String getMsgAnnotated() {
                        return getMsg();
                    }

                    @Override
                    public Collection<? extends AffectedFile> getAffectedFiles() {
                        return Arrays.asList(
                                new AffectedFile() {
                                    public String getPath() {
                                        return "path1";
                                    }

                                    public EditType getEditType() {
                                        return EditType.EDIT;
                                    }
                                },
                                new AffectedFile() {
                                    public String getPath() {
                                        return "path2";
                                    }

                                    public EditType getEditType() {
                                        return EditType.ADD;
                                    }
                                });
                    }
                }).iterator();
            }
        });
    }
}
