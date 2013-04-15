package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import hudson.util.StreamTaskListener;
import org.jvnet.hudson.test.HudsonTestCase;
import org.junit.Test;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JellyScriptContentTest
        extends HudsonTestCase {

    private JellyScriptContent content;
    private ExtendedEmailPublisher publisher;
    private TaskListener listener;

    @Override
    public void setUp()
            throws Exception {
        super.setUp();

        content = new JellyScriptContent();
        listener = new StreamTaskListener(System.out);

        publisher = new ExtendedEmailPublisher();
        publisher.defaultContent = "For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!";
        publisher.defaultSubject = "How would you like your very own AWESOME-O 4000?";
        publisher.recipientList = "ashlux@gmail.com";

        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultBody");
        f.setAccessible(true);
        f.set(ExtendedEmailPublisher.DESCRIPTOR, "Give me $4000 and I'll mail you a check for $40,000!");
        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultSubject");
        f.setAccessible(true);
        f.set(ExtendedEmailPublisher.DESCRIPTOR, "Nigerian needs your help!");

        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("recipientList");
        f.setAccessible(true);
        f.set(ExtendedEmailPublisher.DESCRIPTOR, "ashlux@gmail.com");

        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("hudsonUrl");;;;
        f.setAccessible(true);
        f.set(ExtendedEmailPublisher.DESCRIPTOR, "http://localhost/");
    }

    public void testShouldFindTemplateOnClassPath()
            throws Exception {
        content.template = "empty-template-on-classpath";

        AbstractBuild build = mock(AbstractBuild.class);

        assertEquals("HELLO WORLD!", content.evaluate(build, listener, JellyScriptContent.MACRO_NAME));
    }

    public void testWhenTemplateNotFoundThrowFileNotFoundException()
            throws Exception {
        content.template = "template-does-not-exist";

        AbstractBuild build = mock(AbstractBuild.class);

        String output = content.evaluate(build, listener, JellyScriptContent.MACRO_NAME);

        assertEquals("Jelly script [template-does-not-exist] was not found in $JENKINS_HOME/email-templates.", output);
    }

    /**
     * Makes sure that the rendering of changeset doesn't use a Subversion (or
     * any other SCM) specific methods.
     */
    @Test
    public void testChangeLogDisplayShouldntOnlyRelyOnPortableMethods() throws Exception {
        content.template = "text";

        AbstractBuild build = mock(AbstractBuild.class);
        Mockito.when(build.getTimestamp()).thenReturn(new GregorianCalendar());
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

        AbstractBuild build = mock(AbstractBuild.class);
        Mockito.when(build.getTimestamp()).thenReturn(new GregorianCalendar());
        mockChangeSet(build);

        String output = content.evaluate(build, listener, JellyScriptContent.MACRO_NAME);
        System.out.println(output);

        assertTrue(output.contains("COMMIT MESSAGE"));
        assertTrue(output.contains("Kohsuke Kawaguchi"));
        assertTrue(output.contains("path1"));
        assertTrue(output.contains("path2"));
        assertTrue(output.contains("edit"));
        assertTrue(output.contains("add"));
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
