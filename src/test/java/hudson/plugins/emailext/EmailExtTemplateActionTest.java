package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Test for EmailExtTemplateAction CSP compliance.
 * 
 * @Author Akash Manna
 */
public class EmailExtTemplateActionTest {

    @Test
    @Issue("JENKINS-74891")
    public void testJavaScriptFileIsCSPCompliant() throws Exception {
        java.io.InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("hudson/plugins/emailext/EmailExtTemplateAction/template-test.js");
        assertNotNull(is, "JavaScript file should exist");

        String jsContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        assertFalse(jsContent.contains("innerHTML"), "JavaScript should not use innerHTML (CSP violation)");
        assertFalse(jsContent.contains("escape("), "JavaScript should not use deprecated escape() function");

        assertTrue(jsContent.contains("textContent"), "JavaScript should use textContent instead of innerHTML");
        assertTrue(
                jsContent.contains("encodeURIComponent"), "JavaScript should use encodeURIComponent instead of escape");

        assertTrue(jsContent.contains("fetch("), "JavaScript should use fetch API for AJAX calls");
        assertFalse(
                jsContent.contains("templateTester.renderTemplate"),
                "JavaScript should not use JavaScriptMethod binding (violates CSP)");
    }

    @Test
    @Issue("JENKINS-74891")
    public void testGroovyTemplateDoesNotUseStaplerBind() throws Exception {
        java.io.InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("hudson/plugins/emailext/EmailExtTemplateAction/index.groovy");
        assertNotNull(is, "Groovy template should exist");

        String groovyContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        assertFalse(
                groovyContent.contains("st.bind"),
                "Groovy template should not use st.bind (generates inline JavaScript violating CSP)");
    }
}
