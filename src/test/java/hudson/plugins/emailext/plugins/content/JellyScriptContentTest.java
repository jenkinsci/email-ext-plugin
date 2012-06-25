package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import hudson.tasks.Mailer;
import org.jvnet.hudson.test.HudsonTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JellyScriptContentTest 
    extends HudsonTestCase
{
    private JellyScriptContent jellyScriptContent;

    private Map<String, Object> args;

    private ExtendedEmailPublisher publisher;

    public void setUp()
            throws Exception
    {
        super.setUp();

        jellyScriptContent = new JellyScriptContent();
        args = new HashMap<String, Object>();

        publisher = new ExtendedEmailPublisher();
        publisher.defaultContent = "For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!";
        publisher.defaultSubject = "How would you like your very own AWESOME-O 4000?";
        publisher.recipientList = "ashlux@gmail.com";
        
        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "defaultBody" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, "Give me $4000 and I'll mail you a check for $40,000!" );
        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "defaultSubject" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, "Nigerian needs your help!" );

        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "recipientList" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, "ashlux@gmail.com" );
        
        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "hudsonUrl" );;;;
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, "http://localhost/" );
    }

    public void testShouldFindTemplateOnClassPath()
            throws Exception
    {
        args.put(JellyScriptContent.TEMPLATE_NAME_ARG, "empty-template-on-classpath");

        AbstractBuild build = mock(AbstractBuild.class);

        assertEquals("HELLO WORLD!", jellyScriptContent.getContent(build, null, null, args));
    }

    public void testWhenTemplateNotFoundThrowFileNotFoundException()
            throws Exception
    {
        args.put(JellyScriptContent.TEMPLATE_NAME_ARG, "template-does-not-exist");

        AbstractBuild build = mock(AbstractBuild.class);

        String content = jellyScriptContent.getContent(build, null, null, args);

        assertEquals("Jelly script [template-does-not-exist] was not found in $JENKINS_HOME/email-templates.", content);
    }

    /**
     * Makes sure that the rendering of changeset doesn't use a Subversion (or any other SCM) specific methods.
     */
    @Test
    public void testChangeLogDisplayShouldntOnlyRelyOnPortableMethods() throws Exception {
        args.put(JellyScriptContent.TEMPLATE_NAME_ARG, "text");

        AbstractBuild build = mock(AbstractBuild.class);
        Mockito.when(build.getTimestamp()).thenReturn(new GregorianCalendar());
        mockChangeSet(build);

        String output = jellyScriptContent.getContent(build, null, null, args);
        System.out.println(output);

        assertTrue(output.contains(
                "CHANGE SET\n" +
                "Revision  by Kohsuke Kawaguchi: (COMMIT MESSAGE)\n" +
                "  change: edit path1\n" +
                "  change: add path2\n"));
    }

    /**
     * Makes sure that the rendering of changeset doesn't use a Subversion (or any other SCM) specific methods.
     */
    @Test
    public void testChangeLogDisplayShouldntOnlyRelyOnPortableMethods2() throws Exception {
        args.put(JellyScriptContent.TEMPLATE_NAME_ARG, "html");

        AbstractBuild build = mock(AbstractBuild.class);
        Mockito.when(build.getTimestamp()).thenReturn(new GregorianCalendar());
        mockChangeSet(build);

        String output = jellyScriptContent.getContent(build, null, null, args);
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
