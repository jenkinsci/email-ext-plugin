package hudson.plugins.emailext.plugins.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.util.DescribableList;
import hudson.util.StreamTaskListener;
import java.lang.reflect.Field;
import java.util.GregorianCalendar;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JellyScriptContentTest {

    @Rule
    public JenkinsRule j = new JenkinsRule() {
        @Override
        public void before() throws Throwable {
            super.before();

            content = new JellyScriptContent();
            listener = StreamTaskListener.fromStdout();

            publisher = new ExtendedEmailPublisher();
            publisher.defaultContent = "For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!";
            publisher.defaultSubject = "How would you like your very own AWESOME-O 4000?";
            publisher.recipientList = "ashlux@gmail.com";

            Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultBody");
            f.setAccessible(true);
            f.set(publisher.getDescriptor(), "Give me $4000 and I'll mail you a check for $40,000!");
            f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultSubject");
            f.setAccessible(true);
            f.set(publisher.getDescriptor(), "Nigerian needs your help!");

            f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("recipientList");
            f.setAccessible(true);
            f.set(publisher.getDescriptor(), "ashlux@gmail.com");

            f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("hudsonUrl");
            f.setAccessible(true);
            f.set(publisher.getDescriptor(), "http://localhost/");
            
            build = mock(AbstractBuild.class);
            AbstractProject<?, ?> project = mock(AbstractProject.class);
            DescribableList publishers = mock(DescribableList.class);
            when(publishers.get(ExtendedEmailPublisher.class)).thenReturn(publisher);
            when(project.getPublishersList()).thenReturn(publishers);
            when(build.getProject()).thenReturn(project);
        }
    };
    
    private JellyScriptContent content;
    private ExtendedEmailPublisher publisher;
    private TaskListener listener;
    private AbstractBuild build;

    @Test
    public void testShouldFindTemplateOnClassPath()
            throws Exception {
        content.template = "empty-template-on-classpath";
        assertEquals("HELLO WORLD!", content.evaluate(build, listener, JellyScriptContent.MACRO_NAME));
    }

    @Test
    public void testWhenTemplateNotFoundThrowFileNotFoundException()
            throws Exception {
        content.template = "template-does-not-exist";
        String output = content.evaluate(build, listener, JellyScriptContent.MACRO_NAME);

        assertEquals("Jelly file [template-does-not-exist] was not found in $JENKINS_HOME/email-templates.", output);
    }

    /**
     * Makes sure that the rendering of changeset doesn't use a Subversion (or
     * any other SCM) specific methods.
     */
    @Test
    public void testChangeLogDisplayShouldntOnlyRelyOnPortableMethods() throws Exception {
        content.template = "text";

        when(build.getTimestamp()).thenReturn(new GregorianCalendar());
        mockChangeSet(build);

        String output = content.evaluate(build, listener, JellyScriptContent.MACRO_NAME);

        assertTrue(output.contains(
                "CHANGE SET\n"
                + "Revision  by Kohsuke Kawaguchi: (COMMIT MESSAGE)\n"
                + "  change: edit path1\n"
                + "  change: add path2\n"));
    }

    /**
     * Makes sure that the rendering of changeset doesn't use a Subversion (or
     * any other SCM) specific methods.
     */
    @Test
    public void testChangeLogDisplayShouldntOnlyRelyOnPortableMethods2() throws Exception {
        content.template = "html";

        when(build.getTimestamp()).thenReturn(new GregorianCalendar());
        mockChangeSet(build);

        String output = content.evaluate(build, listener, JellyScriptContent.MACRO_NAME);

        assertTrue(output.contains("COMMIT MESSAGE"));
        assertTrue(output.contains("Kohsuke Kawaguchi"));
        assertTrue(output.contains("path1"));
        assertTrue(output.contains("path2"));
        assertTrue(output.contains("edit"));
        assertTrue(output.contains("add"));
    }

    private void mockChangeSet(final AbstractBuild build) {
        ScriptContentChangeLogSet changeLog = new ScriptContentChangeLogSet(build);
        when(build.getChangeSet()).thenReturn(changeLog);
    }
}
