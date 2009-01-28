package hudson.plugins.emailext;

import org.jmock.MockObjectTestCase;

public class ExtendedEmailPublisherTest extends MockObjectTestCase {

	
	public void setUp(){
		
	}

	public void testSplitCommaSeparatedString(){
		String test = "asdf.fasdfd@fadsf.cadfad, asdfd, adsfadfaife, qwf.235f.adfd.#@adfe.cadfe";
		
		String[] tests = test.split(ExtendedEmailPublisher.COMMA_SEPARATED_SPLIT_REGEXP);
		
		assertEquals(4,tests.length);
	}

	public void testTokenizer1() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("foo");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer2() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("$PROJECT_URL$asdf");
		assertTrue(tokenizer.find());
		assertEquals("PROJECT_URL", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertTrue(tokenizer.find());
		assertEquals("asdf", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertFalse(tokenizer.find());
	}

	public void testTokenizer3() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("$PROJECT_URL/asdf");
		assertTrue(tokenizer.find());
		assertEquals("PROJECT_URL", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertFalse(tokenizer.find());
	}

	public void testTokenizer4() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("${PROJECT_URL}asdf");
		assertTrue(tokenizer.find());
		assertEquals("PROJECT_URL", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertFalse(tokenizer.find());
	}

	public void testTokenizer5() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("${   BUILD_URL\t\t}asdf");
		assertTrue(tokenizer.find());
		assertEquals("BUILD_URL", tokenizer.getTokenName());
		assertEquals(0, tokenizer.getArgs().size());
		assertFalse(tokenizer.find());
	}

	public void testTokenizer6() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("${BUILD_URL,123      ,\t456.789   }");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer7() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("${BUILD_URL, true}");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer8() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("${BUILD_URL, \"a string\"}");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer9() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("${BUILD_URL, \"a string \\\" \\\\ \\' \\n sdfgsdgf\"}");
		assertFalse(tokenizer.find());
	}

	public void testTokenizer10() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer(
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
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer("${PROJECT_URL, BADDY=\"a string \\\" \\\\ \\' \\n sdfgsdgf\"}");
		assertTrue(tokenizer.find());
		assertEquals("PROJECT_URL", tokenizer.getTokenName());
		assertEquals(1, tokenizer.getArgs().size());
		assertEquals("a string \" \\ \' \n sdfgsdgf", tokenizer.getArgs().get("BADDY"));
		assertFalse(tokenizer.find());
	}

	public void testTokenizer12() {
		ExtendedEmailPublisher.Tokenizer tokenizer;
		tokenizer = new ExtendedEmailPublisher.Tokenizer(
				"${BUILD_URL, S=false}\n" +
				"${null,null=null, f= false}");
		assertTrue(tokenizer.find());
		assertEquals("BUILD_URL", tokenizer.getTokenName());
		assertEquals(1, tokenizer.getArgs().size());
		assertEquals(false, tokenizer.getArgs().get("S"));
		assertTrue(tokenizer.find());
		assertEquals("null", tokenizer.getTokenName());
		assertEquals(2, tokenizer.getArgs().size());
		assertEquals(null, tokenizer.getArgs().get("null"));
		assertEquals(false, tokenizer.getArgs().get("f"));
		assertFalse(tokenizer.find());
	}

}
