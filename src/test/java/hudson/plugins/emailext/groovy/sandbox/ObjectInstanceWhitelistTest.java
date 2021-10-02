package hudson.plugins.emailext.groovy.sandbox;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.Test;

/**
 * Tests {@link ObjectInstanceWhitelist}
 */
public class ObjectInstanceWhitelistTest {
    @Test
    public void permitsInstance() {
        String i = "instance";
        ObjectInstanceWhitelist<String> str = new ObjectInstanceWhitelist<String>(i) {};
        assertTrue(str.permitsInstance(i));
    }

    @Test
    public void methodInSubInterface() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        MimeMessageInstanceWhitelist whitelist = new MimeMessageInstanceWhitelist(message);

        Method lineCount = Part.class.getDeclaredMethod("getLineCount");
        assertNotNull(lineCount);
        assertTrue(whitelist.permitsMethod(lineCount, message, new Object[0]));
    }

    @Test
    public void isClass() {
        MimeMessage message = new MimeMessage((Session) null);
        ObjectInstanceWhitelist<MimeMessage> str = new ObjectInstanceWhitelist<MimeMessage>(message) {};
        assertTrue(str.isClass(Part.class));
    }

}
