package hudson.plugins.emailext.plugins;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.util.StreamTaskListener;

import java.io.IOException;
import java.lang.reflect.Field;
import static junit.framework.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;


import static org.mockito.Mockito.*;

public class ContentBuilderTest {

    private ExtendedEmailPublisher publisher;
    private StreamTaskListener listener;
    private AbstractBuild<?, ?> build;
    @Rule
    public JenkinsRule j = new JenkinsRule() {
        @Override
        protected void before() throws Throwable {
            super.before();

            listener = new StreamTaskListener(System.out);

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

            build = mock(AbstractBuild.class);
            when(build.getEnvironment(listener)).thenReturn(new EnvVars());
        }
    };

    @Test
    public void testTransformText_shouldExpand_$PROJECT_DEFAULT_CONTENT()
            throws IOException, InterruptedException {
        assertEquals(publisher.defaultContent, new ContentBuilder().transformText("$PROJECT_DEFAULT_CONTENT", publisher,
                build, listener));
        assertEquals(publisher.defaultContent, new ContentBuilder().transformText("${PROJECT_DEFAULT_CONTENT}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_shouldExpand_$PROJECT_DEFAULT_SUBJECT()
            throws IOException, InterruptedException {
        assertEquals(publisher.defaultSubject, new ContentBuilder().transformText("$PROJECT_DEFAULT_SUBJECT", publisher,
                build, listener));
        assertEquals(publisher.defaultSubject, new ContentBuilder().transformText("${PROJECT_DEFAULT_SUBJECT}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_shouldExpand_$DEFAULT_CONTENT()
            throws IOException, InterruptedException {
        assertEquals(publisher.getDescriptor().getDefaultBody(),
                new ContentBuilder().transformText("$DEFAULT_CONTENT", publisher,
                build, listener));
        assertEquals(publisher.getDescriptor().getDefaultBody(),
                new ContentBuilder().transformText("${DEFAULT_CONTENT}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_shouldExpand_$DEFAULT_SUBJECT()
            throws IOException, InterruptedException {
        assertEquals(publisher.getDescriptor().getDefaultSubject(),
                new ContentBuilder().transformText("$DEFAULT_SUBJECT", publisher,
                build, listener));
        assertEquals(publisher.getDescriptor().getDefaultSubject(),
                new ContentBuilder().transformText("${DEFAULT_SUBJECT}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_shouldExpand_$DEFAULT_RECIPIENT_LIST()
            throws IOException, InterruptedException {
        assertEquals(publisher.getDescriptor().getDefaultRecipients(),
                new ContentBuilder().transformText("$DEFAULT_RECIPIENTS", publisher,
                build, listener));
        assertEquals(publisher.getDescriptor().getDefaultRecipients(),
                new ContentBuilder().transformText("${DEFAULT_RECIPIENTS}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_shouldExpand_$DEFAULT_PRESEND_SCRIPT()
            throws IOException, InterruptedException {
        assertEquals(publisher.getDescriptor().getDefaultPresendScript(),
                new ContentBuilder().transformText("$DEFAULT_PRESEND_SCRIPT", publisher,
                build, listener));
        assertEquals(publisher.getDescriptor().getDefaultPresendScript(),
                new ContentBuilder().transformText("${DEFAULT_PRESEND_SCRIPT}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_noNPEWithNullDefaultSubjectBody() throws NoSuchFieldException, IllegalAccessException {
        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultBody");
        f.setAccessible(true);
        f.set(publisher.getDescriptor(), null);
        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultSubject");
        f.setAccessible(true);
        f.set(publisher.getDescriptor(), null);
        assertEquals("", new ContentBuilder().transformText("$DEFAULT_SUBJECT", publisher, build, listener));
        assertEquals("", new ContentBuilder().transformText("$DEFAULT_CONTENT", publisher, build, listener));
    }

    @Test
    public void testEscapedToken() throws IOException, InterruptedException {
        build = mock(AbstractBuild.class);
        EnvVars testVars = new EnvVars();
        testVars.put("FOO", "BAR");
        when(build.getEnvironment(listener)).thenReturn(testVars);

        assertEquals("\\BAR", new ContentBuilder().transformText("\\${ENV, var=\"FOO\"}", publisher, build, listener));
    }
}
