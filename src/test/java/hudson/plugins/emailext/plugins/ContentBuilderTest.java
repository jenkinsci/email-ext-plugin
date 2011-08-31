package hudson.plugins.emailext.plugins;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;

import java.lang.reflect.Field;

import junit.framework.TestSuite;

import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jvnet.hudson.test.HudsonTestCase;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit3.PowerMockSuite;

@PrepareForTest(TokenMacro.class)
public class ContentBuilderTest extends HudsonTestCase {

    private ExtendedEmailPublisher publisher;

    @SuppressWarnings("unchecked")
    public static TestSuite suite() throws Exception {
        return new PowerMockSuite(ContentBuilderTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        publisher = mock(ExtendedEmailPublisher.class);
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
    }

	public void testTokenizer1() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("foo");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer2() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("$PROJECT_URL$asdf");
		assertTrue(tokenizer.find());
		assertEquals("PROJECT_URL", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertTrue(tokenizer.find());
		assertEquals("asdf", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertFalse(tokenizer.find());
	}

	public void testTokenizer3() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("$PROJECT_URL/asdf");
		assertTrue(tokenizer.find());
		assertEquals("PROJECT_URL", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertFalse(tokenizer.find());
	}

	public void testTokenizer4() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("${PROJECT_URL}asdf");
		assertTrue(tokenizer.find());
		assertEquals("PROJECT_URL", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertFalse(tokenizer.find());
	}

	public void testTokenizer5() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("${   BUILD_URL\t\t}asdf");
		assertTrue(tokenizer.find());
		assertEquals("BUILD_URL", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertFalse(tokenizer.find());
	}

	public void testTokenizer6() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("${BUILD_URL,123      ,\t456.789   }");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer7() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("${BUILD_URL, true}");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer8() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("${BUILD_URL, \"a string\"}");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer9() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("${BUILD_URL, \"a string \\\" \\\\ \\' \\n sdfgsdgf\"}");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer10() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer(
				"${BUILD_URL,a1=123      ,\t  arg_two =\t 456.789   }\n" +
				"${ELSE, S=true, s=\"a string\"}\n");
		assertTrue(tokenizer.find());
		assertEquals("BUILD_URL", tokenizer.getTokenName());
		assertEquals(2, tokenizer.getArgs().size());
		assertEquals(123, tokenizer.getArgs().get("a1"));
		assertEquals(456.789f, tokenizer.getArgs().get("arg_two"));
		assertTrue(tokenizer.find());
		assertEquals("ELSE", tokenizer.getTokenName());
		assertEquals(2, tokenizer.getArgs().size());
		assertEquals(true, tokenizer.getArgs().get("S"));
		assertEquals("a string", tokenizer.getArgs().get("s"));
		assertFalse(tokenizer.find());
	}

	public void testTokenizer11() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer("${PROJECT_URL, BADDY=\"a string \\\" \\\\ \\' \\n sdfgsdgf\"}");
		assertTrue(tokenizer.find());
		assertEquals("PROJECT_URL", tokenizer.getTokenName());
		assertEquals(1, tokenizer.getArgs().size());
		assertEquals("a string \" \\ \' \n sdfgsdgf", tokenizer.getArgs().get("BADDY"));
		assertFalse(tokenizer.find());
	}

	public void testTokenizer12() {
		ContentBuilder.Tokenizer tokenizer;
		tokenizer = new ContentBuilder.Tokenizer(
				"${BUILD_URL, S=false}\n" +
				"${null,null=true, f= false}");
		assertTrue(tokenizer.find());
		assertEquals("BUILD_URL", tokenizer.getTokenName());
		assertEquals(1, tokenizer.getArgs().size());
		assertEquals(false, tokenizer.getArgs().get("S"));
		assertTrue(tokenizer.find());
		assertEquals("null", tokenizer.getTokenName());
		assertEquals(2, tokenizer.getArgs().size());
		assertEquals(true, tokenizer.getArgs().get("null"));
		assertEquals(false, tokenizer.getArgs().get("f"));
		assertFalse(tokenizer.find());
	}

    public void testTransformText_shouldExpand_$PROJECT_DEFAULT_CONTENT() {
        assertEquals(publisher.defaultContent, new ContentBuilder().transformText( "$PROJECT_DEFAULT_CONTENT", publisher, null,
                                                       mock( AbstractBuild.class ), mock(BuildListener.class) ));
        assertEquals(publisher.defaultContent, new ContentBuilder().transformText( "${PROJECT_DEFAULT_CONTENT}", publisher, null,
                                                       mock( AbstractBuild.class ), mock(BuildListener.class) ));
    }

    public void testTransformText_shouldExpand_$PROJECT_DEFAULT_SUBJECT() {
        assertEquals(publisher.defaultSubject, new ContentBuilder().transformText( "$PROJECT_DEFAULT_SUBJECT", publisher, null,
                                                       mock( AbstractBuild.class ), mock(BuildListener.class) ));
        assertEquals(publisher.defaultSubject, new ContentBuilder().transformText( "${PROJECT_DEFAULT_SUBJECT}", publisher, null,
                                                       mock( AbstractBuild.class ), mock(BuildListener.class) ));
    }


    public void testTransformText_shouldExpand_$PROJECT_DEFAULT_RECIPIENTS() {
    	assertEquals(publisher.recipientList, new ContentBuilder().transformText( "$PROJECT_DEFAULT_RECIPIENTS", publisher, null,
    													mock( AbstractBuild.class ), mock(BuildListener.class) ));
    	assertEquals(publisher.recipientList, new ContentBuilder().transformText( "${PROJECT_DEFAULT_RECIPIENTS}", publisher, null,
														mock( AbstractBuild.class ), mock(BuildListener.class) ));
    }

    public void testTransformText_shouldExpand_$DEFAULT_CONTENT() {
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultBody(),
                      new ContentBuilder().transformText( "$DEFAULT_CONTENT", publisher, null,
                                                          mock( AbstractBuild.class ), mock(BuildListener.class) ) );
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultBody(),
                      new ContentBuilder().transformText( "${DEFAULT_CONTENT}", publisher, null,
                                                          mock( AbstractBuild.class ), mock(BuildListener.class) ) );
    }

    public void testTransformText_shouldExpand_$DEFAULT_SUBJECT() {
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultSubject(),
                      new ContentBuilder().transformText( "$DEFAULT_SUBJECT", publisher, null,
                                                          mock( AbstractBuild.class ), mock(BuildListener.class) ) );
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultSubject(),
                      new ContentBuilder().transformText( "${DEFAULT_SUBJECT}", publisher, null,
                                                          mock( AbstractBuild.class ), mock(BuildListener.class) ) );
    }

    public void testTransformText_shouldExpand_$DEFAULT_RECIPIENT_LIST() {
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultRecipients(),
                  new ContentBuilder().transformText( "$DEFAULT_RECIPIENTS", publisher, null,
                                                      mock( AbstractBuild.class ), mock(BuildListener.class) ) );
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultRecipients(),
                  new ContentBuilder().transformText( "${DEFAULT_RECIPIENTS}", publisher, null,
                                                      mock( AbstractBuild.class ), mock(BuildListener.class) ) );
    }

    public void testTokenMacroExpansion() throws Exception {
        mockStatic(TokenMacro.class);

        when(TokenMacro.expand(any(AbstractBuild.class), any(TaskListener.class), eq("$FINDBUGS_FIXED"))).thenReturn("3");
        assertEquals("3", new ContentBuilder().transformText("$FINDBUGS_FIXED", publisher, null, mock(AbstractBuild.class),
                mock(BuildListener.class)));

        when(TokenMacro.expand(any(AbstractBuild.class), any(TaskListener.class), eq("${FINDBUGS_FIXED}"))).thenReturn("4");
        assertEquals("4", new ContentBuilder().transformText("${FINDBUGS_FIXED}", publisher, null, mock(AbstractBuild.class),
                mock(BuildListener.class)));

        publisher.defaultContent = "Findbugs fixed: $FINDBUGS_FIXED";
        when(TokenMacro.expand(any(AbstractBuild.class), any(TaskListener.class), anyString())).thenReturn("Findbugs fixed: 5");
        String str = new ContentBuilder().transformText("$PROJECT_DEFAULT_CONTENT", publisher, null, mock(AbstractBuild.class),
                mock(BuildListener.class));
        assertEquals("Findbugs fixed: 5", str);
    }

}
