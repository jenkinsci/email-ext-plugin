package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Test for EmailExtTemplateAction CSP compliance.
 *
 * @Author Akash Manna
 */
@WithJenkins
public class EmailExtTemplateActionTest {

    @Test
    @Issue("JENKINS-74891")
    public void testJavaScriptUsesFetchAPI() throws Exception {
        java.io.InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("hudson/plugins/emailext/EmailExtTemplateAction/template-test.js");
        assertNotNull(is, "JavaScript file should exist");

        String jsContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        assertFalse(jsContent.contains("innerHTML"), "JavaScript should not use innerHTML (CSP violation)");
        assertFalse(jsContent.contains("escape("), "JavaScript should not use deprecated escape() function");
        assertFalse(
                jsContent.contains("templateTester.renderTemplate"),
                "JavaScript should not use JavaScriptMethod binding (violates CSP)");

        assertTrue(jsContent.contains("textContent"), "JavaScript should use textContent instead of innerHTML");
        assertTrue(
                jsContent.contains("encodeURIComponent"), "JavaScript should use encodeURIComponent instead of escape");
        assertTrue(jsContent.contains("fetch("), "JavaScript should use fetch API for AJAX calls");

        assertTrue(
                jsContent.contains("data-root-url"), "JavaScript should use data-root-url for absolute URL resolution");
        assertTrue(
                jsContent.contains("data-project-url"),
                "JavaScript should use data-project-url for project path resolution");

        assertTrue(jsContent.contains("HTTP"), "JavaScript should include HTTP status code in error messages");
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

    @Test
    @Issue("JENKINS-74891")
    public void testRenderTemplateEndpointExists(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-project");
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        assertTrue(
                java.util.Arrays.stream(action.getClass().getDeclaredMethods())
                        .anyMatch(m -> m.getName().equals("doRenderTemplate")),
                "EmailExtTemplateAction should have doRenderTemplate endpoint");

        assertTrue(
                action.getUrlName().equals("templateTest"),
                "Action URL name should be 'templateTest' but got: " + action.getUrlName());
    }

    @Test
    @Issue("JENKINS-74891")
    public void testEndpointHasRequirePostAnnotation() throws Exception {
        java.lang.reflect.Method[] methods = EmailExtTemplateAction.class.getDeclaredMethods();
        java.lang.reflect.Method renderMethod = null;
        for (java.lang.reflect.Method m : methods) {
            if (m.getName().equals("doRenderTemplate")) {
                renderMethod = m;
                break;
            }
        }

        assertNotNull(renderMethod, "doRenderTemplate method should exist");
        assertTrue(
                renderMethod.isAnnotationPresent(org.kohsuke.stapler.interceptor.RequirePOST.class),
                "doRenderTemplate should have @RequirePOST annotation for CSRF protection");

        assertFalse(
                renderMethod.isAnnotationPresent(SuppressWarnings.class),
                "doRenderTemplate should not have @SuppressWarnings annotation since @RequirePOST handles CSRF");
    }
}
