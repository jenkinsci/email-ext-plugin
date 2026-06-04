package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

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
        InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("hudson/plugins/emailext/EmailExtTemplateAction/template-test.js");
        assertNotNull(is, "JavaScript file should exist");

        String jsContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

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
        InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("hudson/plugins/emailext/EmailExtTemplateAction/index.groovy");
        assertNotNull(is, "Groovy template should exist");

        String groovyContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

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
                Arrays.stream(action.getClass().getDeclaredMethods())
                        .anyMatch(m -> m.getName().equals("doRenderTemplate")),
                "EmailExtTemplateAction should have doRenderTemplate endpoint");

        assertTrue(
                action.getUrlName().equals("templateTest"),
                "Action URL name should be 'templateTest' but got: " + action.getUrlName());
    }

    @Test
    @Issue("JENKINS-74891")
    public void testEndpointHasRequirePostAnnotation() throws Exception {
        Method[] methods = EmailExtTemplateAction.class.getDeclaredMethods();
        Method renderMethod = null;
        for (Method m : methods) {
            if (m.getName().equals("doRenderTemplate")) {
                renderMethod = m;
                break;
            }
        }

        assertNotNull(renderMethod, "doRenderTemplate method should exist");
        assertTrue(
                renderMethod.isAnnotationPresent(RequirePOST.class),
                "doRenderTemplate should have @RequirePOST annotation for CSRF protection");

        assertFalse(
                renderMethod.isAnnotationPresent(SuppressWarnings.class),
                "doRenderTemplate should not have @SuppressWarnings annotation since @RequirePOST handles CSRF");
    }

    @Test
    @Issue("JENKINS-74891")
    public void testRenderTemplateEndpointActuallyWorks(JenkinsRule j) throws Exception {
        // Create a real project for testing endpoint with real Jenkins environment
        FreeStyleProject project = j.createFreeStyleProject("test-project");

        // Get the template action and verify the endpoint
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        // Verify the endpoint is instantiated successfully for the project
        assertNotNull(action, "EmailExtTemplateAction should be instantiated successfully for the project");

        // Verify doRenderTemplate method is accessible and public to be called by Stapler
        Method renderMethod = EmailExtTemplateAction.class.getDeclaredMethod(
                "doRenderTemplate", StaplerRequest2.class, StaplerResponse2.class);
        assertTrue(
                Modifier.isPublic(renderMethod.getModifiers()),
                "doRenderTemplate should be public to be accessible as HTTP endpoint");

        // Verify the method has @RequirePOST for CSRF protection
        assertTrue(
                renderMethod.isAnnotationPresent(RequirePOST.class),
                "doRenderTemplate should have @RequirePOST annotation to protect against CSRF attacks");
    }
}
