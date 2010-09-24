package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

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

    @Test(expected = FileNotFoundException.class)
    public void testWhenTemplateNotFoundThrowFileNotFoundException()
            throws Exception
    {
        mockHudsonGetRootDir(new File("."));

        args.put(JellyScriptContent.TEMPLATE_NAME_ARG, "template-does-not-exist");

        AbstractBuild build = mock(AbstractBuild.class);

        jellyScriptContent.getContent(build, null, null, args);
    }

    private void mockHudsonGetRootDir(File rootDir)
    {
        PowerMockito.mockStatic(Hudson.class);
        final Hudson hudson = PowerMockito.mock(Hudson.class);
        Mockito.when(Hudson.getInstance()).thenReturn(hudson);
        PowerMockito.when(hudson.getRootDir()).thenReturn(rootDir);
    }
}
