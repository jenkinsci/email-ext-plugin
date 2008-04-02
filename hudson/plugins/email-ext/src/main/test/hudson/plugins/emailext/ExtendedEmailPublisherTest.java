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

	
}
