package hudson.plugins.emailext.plugins;

import com.google.common.collect.ListMultimap;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import static junit.framework.Assert.assertEquals;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;


import static org.mockito.Mockito.*;

public class ContentBuilderTest {

    private ExtendedEmailPublisher publisher;
    private BuildListener listener;
    private AbstractBuild<?, ?> build;
    @Rule
    public JenkinsRule j = new JenkinsRule() {
        @Override
        public void before() throws Throwable {
            super.before();

            listener = new StreamBuildListener(System.out);

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
        assertEquals(publisher.defaultContent, ContentBuilder.transformText("$PROJECT_DEFAULT_CONTENT", publisher,
                build, j.createLocalLauncher(), listener));
        assertEquals(publisher.defaultContent, ContentBuilder.transformText("${PROJECT_DEFAULT_CONTENT}", publisher,
                build, j.createLocalLauncher(), listener));
    }

    @Test
    public void testTransformText_shouldExpand_$PROJECT_DEFAULT_SUBJECT()
            throws IOException, InterruptedException {
        assertEquals(publisher.defaultSubject, ContentBuilder.transformText("$PROJECT_DEFAULT_SUBJECT", publisher,
                build, listener));
        assertEquals(publisher.defaultSubject, ContentBuilder.transformText("${PROJECT_DEFAULT_SUBJECT}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_shouldExpand_$DEFAULT_CONTENT()
            throws IOException, InterruptedException {
        assertEquals(publisher.getDescriptor().getDefaultBody(),
                ContentBuilder.transformText("$DEFAULT_CONTENT", publisher,
                build, listener));
        assertEquals(publisher.getDescriptor().getDefaultBody(),
                ContentBuilder.transformText("${DEFAULT_CONTENT}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_shouldExpand_$DEFAULT_SUBJECT()
            throws IOException, InterruptedException {
        assertEquals(publisher.getDescriptor().getDefaultSubject(),
                ContentBuilder.transformText("$DEFAULT_SUBJECT", publisher,
                build, listener));
        assertEquals(publisher.getDescriptor().getDefaultSubject(),
                ContentBuilder.transformText("${DEFAULT_SUBJECT}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_shouldExpand_$DEFAULT_RECIPIENT_LIST()
            throws IOException, InterruptedException {
        assertEquals(publisher.getDescriptor().getDefaultRecipients(),
                ContentBuilder.transformText("$DEFAULT_RECIPIENTS", publisher,
                build, listener));
        assertEquals(publisher.getDescriptor().getDefaultRecipients(),
                ContentBuilder.transformText("${DEFAULT_RECIPIENTS}", publisher,
                build, listener));
    }

    @Test
    public void testTransformText_shouldExpand_$DEFAULT_PRESEND_SCRIPT()
            throws IOException, InterruptedException {
        assertEquals(publisher.getDescriptor().getDefaultPresendScript(),
                ContentBuilder.transformText("$DEFAULT_PRESEND_SCRIPT", publisher,
                build, listener));
        assertEquals(publisher.getDescriptor().getDefaultPresendScript(),
                ContentBuilder.transformText("${DEFAULT_PRESEND_SCRIPT}", publisher,
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
        assertEquals("", ContentBuilder.transformText("$DEFAULT_SUBJECT", publisher, build, listener));
        assertEquals("", ContentBuilder.transformText("$DEFAULT_CONTENT", publisher, build, listener));
    }

    @Test
    public void testEscapedToken() throws IOException, InterruptedException {
        build = mock(AbstractBuild.class);
        EnvVars testVars = new EnvVars();
        testVars.put("FOO", "BAR");
        when(build.getEnvironment(listener)).thenReturn(testVars);

        assertEquals("\\BAR", ContentBuilder.transformText("\\${ENV, var=\"FOO\"}", publisher, build, listener));
    }
    
    @Test
    public void testRuntimeMacro() throws IOException, InterruptedException {
        RuntimeContent content = new RuntimeContent("Hello, world");
        assertEquals("Hello, world", ContentBuilder.transformText("${RUNTIME}", new ExtendedEmailPublisherContext(publisher, build, j.createLocalLauncher(), listener), Collections.singletonList((TokenMacro)content)));
    }
    
    public class RuntimeContent extends TokenMacro {
        
        public static final String MACRO_NAME = "RUNTIME";
        private final String replacement;
        
        public RuntimeContent(String replacement) {
            this.replacement = replacement;
        }

        @Override
        public boolean acceptsMacroName(String name) {
            return name.equals(MACRO_NAME);
        }

        @Override
        public String evaluate(Run<?, ?> ab, FilePath workspace, TaskListener tl, String string, Map<String, String> map, ListMultimap<String, String> lm) throws MacroEvaluationException, IOException, InterruptedException {
            return replacement;
        }        
    }
}
