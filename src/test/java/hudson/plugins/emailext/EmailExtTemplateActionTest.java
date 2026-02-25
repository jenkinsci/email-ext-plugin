package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Test for EmailExtTemplateAction CSP compliance.
 */
public class EmailExtTemplateActionTest {

    @Test
    @Issue("JENKINS-74891")
    public void testJavaScriptFileIsCSPCompliant() throws Exception {
        java.io.InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("hudson/plugins/emailext/EmailExtTemplateAction/template-test.js");
        assertNotNull(is);

        String jsContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        assertFalse(jsContent.contains("innerHTML"));
        assertFalse(jsContent.contains("escape("));
        assertTrue(jsContent.contains("textContent"));
        assertTrue(jsContent.contains("encodeURIComponent"));
    }
}
