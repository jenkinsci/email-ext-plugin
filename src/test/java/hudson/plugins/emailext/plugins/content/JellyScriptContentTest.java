package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.plugins.content.ChangesSinceLastBuildContentTest.ChangeLogEntry;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {ExtendedEmailPublisherDescriptor.class, ExtendedEmailPublisher.class, Hudson.class})
public class JellyScriptContentTest
{
    private JellyScriptContent jellyScriptContent;

    private Map<String, Object> args;

    @Before
    public void setup()
            throws Exception
    {
        jellyScriptContent = new JellyScriptContent();

        args = new HashMap<String, Object>();

        mockDescriptorConstructor();
    }

    private void mockDescriptorConstructor()
            throws Exception
    {
        ExtendedEmailPublisherDescriptor descriptor = PowerMockito.mock(ExtendedEmailPublisherDescriptor.class);
        PowerMockito.whenNew(ExtendedEmailPublisherDescriptor.class).withNoArguments().thenReturn(descriptor);
    }

    @Test
    public void testShouldFindTemplateOnClassPath()
            throws Exception
    {
        args.put(JellyScriptContent.TEMPLATE_NAME_ARG, "empty-template-on-classpath");

        AbstractBuild build = mock(AbstractBuild.class);

        assertEquals("HELLO WORLD!", jellyScriptContent.getContent(build, null, null, args));
    }

    @Test
    public void testWhenTemplateNotFoundThrowFileNotFoundException()
            throws Exception
    {
        mockHudsonGetRootDir(new File("."));

        args.put(JellyScriptContent.TEMPLATE_NAME_ARG, "template-does-not-exist");

        AbstractBuild build = mock(AbstractBuild.class);

        String content = jellyScriptContent.getContent(build, null, null, args);

        assertEquals("Jelly script [template-does-not-exist] was not found in $JENKINS_HOME/email-templates.", content);
    }

    private void mockHudsonGetRootDir(File rootDir)
    {
        PowerMockito.mockStatic(Hudson.class);
        final Hudson hudson = PowerMockito.mock(Hudson.class);
        Mockito.when(Hudson.getInstance()).thenReturn(hudson);
        PowerMockito.when(hudson.getRootDir()).thenReturn(rootDir);
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
