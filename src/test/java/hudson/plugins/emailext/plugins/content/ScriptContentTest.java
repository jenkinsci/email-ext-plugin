package hudson.plugins.emailext.plugins.content;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@SuppressWarnings({"unchecked"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {ExtendedEmailPublisherDescriptor.class, ExtendedEmailPublisher.class, Hudson.class})
public class ScriptContentTest
{
    private ScriptContent scriptContent;

    private Map<String, Object> args;

    private final String osName = System.getProperty("os.name");

    private final boolean osIsDarwin = osName.equals("Mac OS X") || osName.equals("Darwin");

    @Before
    public void setup()
            throws Exception
    {
        assumeThat(osIsDarwin, is(false));

        scriptContent = new ScriptContent();

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
    public void testShouldFindScriptOnClassPath()
            throws Exception
    {
        args.put(ScriptContent.SCRIPT_NAME_ARG, "empty-script-on-classpath.groovy");

        AbstractBuild build = mock(AbstractBuild.class);

        assertEquals("HELLO WORLD!", scriptContent.getContent(build, null, null, args));
    }

    /*
    @Test
    public void testShouldFindTemplateOnClassPath()
        throws Exception
    {
        args.put(ScriptContent.SCRIPT_TEMPLATE_ARG, "empty-groovy-template-on-classpath.template");

        AbstractBuild build = mock(AbstractBuild.class);

        assertEquals("HELLO WORLD!", scriptContent.getContent(build, null, null, args));
    }
    */

    @Test
    public void testWhenScriptNotFoundThrowFileNotFoundException()
            throws Exception
    {
        mockHudsonGetRootDir(new File("."));

        args.put(ScriptContent.SCRIPT_NAME_ARG, "script-does-not-exist");

        AbstractBuild build = mock(AbstractBuild.class);

        String content = scriptContent.getContent(build, null, null, args);

        assertEquals("Script [script-does-not-exist] or template [groovy-html.template] was not found in $JENKINS_HOME/email-templates.", content);
    }

    @Test
    public void testWhenTemplateNotFoundThrowFileNotFoundException()
            throws Exception
    {
        mockHudsonGetRootDir(new File("."));

        args.put(ScriptContent.SCRIPT_TEMPLATE_ARG, "template-does-not-exist");

        AbstractBuild build = mock(AbstractBuild.class);

        String content = scriptContent.getContent(build, null, null, args);

        assertEquals("Script [email-ext.groovy] or template [template-does-not-exist] was not found in $JENKINS_HOME/email-templates.", content);
    }
    /**
	 * this is for groovy template testing 
	 * @throws Exception
	 */
	@Test
	public void testWithGroovyTemplate() throws Exception {
		args.put(ScriptContent.SCRIPT_TEMPLATE_ARG, "groovy-sample.template");
		args.put(ScriptContent.SCRIPT_INIT_ARG, false);

		// mock the build 
		AbstractBuild build = mock(AbstractBuild.class);
		when(build.getResult()).thenReturn(Result.SUCCESS);
		when(build.getUrl()).thenReturn("email-test/34");
		ExtendedEmailPublisher emailPublisher = new ExtendedEmailPublisher();
		when(emailPublisher.DESCRIPTOR.getHudsonUrl()).thenReturn("http://localhost/");	
		
		// mock changeSet
		mockChangeSet(build);
		
		// generate result from groovy template
		String content = scriptContent.getContent(build, emailPublisher, null, args);

		// read expected file in resource to easy compare
		String expectedFile = "hudson/plugins/emailext/templates/" + "groovy-sample.result";
		InputStream in = getClass().getClassLoader().getResourceAsStream(expectedFile);
		String expected = new Scanner(in).useDelimiter("\\Z").next();
		
		// remove end space before compare
		assertEquals(expected.trim(), content.trim());
	}
    private void mockHudsonGetRootDir(File rootDir)
    {
        PowerMockito.mockStatic(Hudson.class);
        final Hudson hudson = PowerMockito.mock(Hudson.class);
        Mockito.when(Hudson.getInstance()).thenReturn(hudson);
        PowerMockito.when(hudson.getRootDir()).thenReturn(rootDir);
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
