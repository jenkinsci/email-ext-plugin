package hudson.plugins.emailext.groovy.sandbox;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ObjectInstanceWhitelist}
 */
class ObjectInstanceWhitelistTest {

    @Test
    void permitsInstance() {
        String i = "instance";
        ObjectInstanceWhitelist<String> str = new ObjectInstanceWhitelist<>(i) {};
        assertTrue(str.permitsInstance(i));
    }

    @Test
    void methodInSubInterface() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        MimeMessageInstanceWhitelist whitelist = new MimeMessageInstanceWhitelist(message);

        Method lineCount = Part.class.getDeclaredMethod("getLineCount");
        assertNotNull(lineCount);
        assertTrue(whitelist.permitsMethod(lineCount, message, new Object[0]));
    }

    @Test
    void isClass() {
        MimeMessage message = new MimeMessage((Session) null);
        ObjectInstanceWhitelist<MimeMessage> str = new ObjectInstanceWhitelist<>(message) {};
        assertTrue(str.isClass(Part.class));
    }
}
