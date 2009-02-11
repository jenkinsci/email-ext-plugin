package hudson.plugins.emailext;

import junit.framework.TestCase;

public class EmailTypeTest extends TestCase {

	public void testHasNoRecipients(){
		EmailType t = new EmailType();
		
		t.setSendToRecipientList(false);
		t.setSendToDevelopers(false);
		
		assertFalse(t.getHasRecipients());
	}
	
	public void testHasDeveloperRecipients(){
		EmailType t = new EmailType();
		
		t.setSendToRecipientList(false);
		t.setSendToDevelopers(true);
		
		assertTrue(t.getHasRecipients());
	}
	
	public void testHasRecipientList(){
		EmailType t = new EmailType();
		
		t.setSendToRecipientList(true);
		t.setSendToDevelopers(false);
		
		assertTrue(t.getHasRecipients());
	}
	
	public void testHasDeveloperAndRecipientList(){
		EmailType t = new EmailType();
		
		t.setSendToRecipientList(true);
		t.setSendToDevelopers(true);
		
		assertTrue(t.getHasRecipients());
	}



}
