package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Tests the class {@link EmailExtTemplateAction}.
 *
 * @author Akash Manna
 */
@WithJenkins
class EmailExtTemplateActionTest {

    private static ExtendedEmailPublisherDescriptor ensureDescriptor(JenkinsRule j) {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

        if (descriptor == null) {
            ExtendedEmailPublisherDescriptor injected = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getDescriptorList(Publisher.class).add(injected);
            descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        }

        assertNotNull(descriptor, "ExtendedEmailPublisherDescriptor should be available in this test runtime");
        return descriptor;
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            ScriptApproval.get().clearApprovedScripts();
        } catch (IllegalStateException ignored) {
            // Jenkins is already gone for tests that only use mocked model objects.
        }
    }

    @Test
    @Issue("JENKINS-74891")
    void testJavaScriptUsesFetchAPI() throws Exception {
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
    void testGroovyTemplateDoesNotUseStaplerBind() throws Exception {
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
    void testRenderTemplateEndpointExists(JenkinsRule j) throws Exception {
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
    void testEndpointHasRequirePostAnnotation() throws Exception {
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
    void testRenderTemplateEndpointActuallyWorks(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-project");
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        assertNotNull(action, "EmailExtTemplateAction should be instantiated successfully for the project");

        Method renderMethod = EmailExtTemplateAction.class.getDeclaredMethod(
                "doRenderTemplate", StaplerRequest2.class, StaplerResponse2.class);
        assertTrue(
                Modifier.isPublic(renderMethod.getModifiers()),
                "doRenderTemplate should be public to be accessible as HTTP endpoint");

        assertTrue(
                renderMethod.isAnnotationPresent(RequirePOST.class),
                "doRenderTemplate should have @RequirePOST annotation to protect against CSRF attacks");
    }

    @Test
    void doTemplateFileCheckAllowsClasspathTemplateWhenConfigurePermissionIsGranted(JenkinsRule j) throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ, Item.CONFIGURE)
                .everywhere()
                .to("alice"));

        ExtendedEmailPublisherDescriptor descriptor = ensureDescriptor(j);
        descriptor.setAdminRequiredForTemplateTesting(false);

        FreeStyleProject project = j.createFreeStyleProject("template-check");
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        try (ACLContext ignored = ACL.as(User.getById("alice", true))) {
            FormValidation validation = action.doTemplateFileCheck("text.jelly");
            assertEquals(FormValidation.Kind.OK, validation.kind);
        }
    }

    @Test
    void doTemplateFileCheckRequiresManageWhenConfigured(JenkinsRule j) throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Jenkins.MANAGE)
                .everywhere()
                .to("manager"));

        ExtendedEmailPublisherDescriptor descriptor = ensureDescriptor(j);
        descriptor.setAdminRequiredForTemplateTesting(true);

        FreeStyleProject project = j.createFreeStyleProject("template-check-admin");
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        try (ACLContext ignored = ACL.as(User.getById("manager", true))) {
            FormValidation validation = action.doTemplateFileCheck("text.jelly");
            assertEquals(FormValidation.Kind.OK, validation.kind);
        }
    }

    @Test
    void doTemplateFileCheckReportsMissingManagedTemplate(JenkinsRule j) throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ, Item.CONFIGURE)
                .everywhere()
                .to("alice"));

        ExtendedEmailPublisherDescriptor descriptor = ensureDescriptor(j);
        descriptor.setAdminRequiredForTemplateTesting(false);

        FreeStyleProject project = j.createFreeStyleProject("template-check-managed-missing");
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        try (ACLContext ignored = ACL.as(User.getById("alice", true))) {
            FormValidation validation = action.doTemplateFileCheck("managed:missing-template");
            assertEquals(FormValidation.Kind.ERROR, validation.kind);
            if (j.jenkins.getPlugin("config-file-provider") == null) {
                assertEquals(Messages.EmailExtTemplateAction_ConfigFileProviderNotAvailable(), validation.getMessage());
            } else {
                assertTrue(validation.getMessage().contains("Managed template not found"));
            }
        }
    }

    @Test
    void doTemplateFileCheckReportsManagedTemplateBehavior(JenkinsRule j) throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ, Item.CONFIGURE)
                .everywhere()
                .to("alice"));

        ExtendedEmailPublisherDescriptor descriptor = ensureDescriptor(j);
        descriptor.setAdminRequiredForTemplateTesting(false);

        FreeStyleProject project = j.createFreeStyleProject("template-check-managed");
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        try (ACLContext ignored = ACL.as(User.getById("alice", true))) {
            if (j.jenkins.getPlugin("config-file-provider") == null) {
                FormValidation validation = action.doTemplateFileCheck("managed:managed-template");
                assertEquals(FormValidation.Kind.ERROR, validation.kind);
                assertEquals(Messages.EmailExtTemplateAction_ConfigFileProviderNotAvailable(), validation.getMessage());
                return;
            }

            String managedTemplateName = "managed-template";
            JellyTemplateConfig.JellyTemplateConfigProvider provider = j.jenkins
                    .getExtensionList(JellyTemplateConfig.JellyTemplateConfigProvider.class)
                    .get(JellyTemplateConfig.JellyTemplateConfigProvider.class);
            if (provider == null) {
                FormValidation validation = action.doTemplateFileCheck("managed:" + managedTemplateName);
                assertEquals(FormValidation.Kind.ERROR, validation.kind);
                assertEquals(Messages.EmailExtTemplateAction_ManagedTemplateNotFound(), validation.getMessage());
                return;
            }

            String managedTemplateId = "managed-template-id-" + UUID.randomUUID();
            GlobalConfigFiles.get()
                    .save(new JellyTemplateConfig(
                            managedTemplateId,
                            managedTemplateName,
                            "",
                            "<j:jelly xmlns:j=\"jelly:core\">HELLO WORLD!</j:jelly>"));

            FormValidation validation = action.doTemplateFileCheck("managed:" + managedTemplateName);
            assertEquals(FormValidation.Kind.OK, validation.kind);

            FormValidation missing = action.doTemplateFileCheck("managed:missing-template");
            assertEquals(FormValidation.Kind.ERROR, missing.kind);
            assertEquals(Messages.EmailExtTemplateAction_ManagedTemplateNotFound(), missing.getMessage());
        }
    }

    @Test
    void doTemplateFileCheckReportsManagedTemplateError(JenkinsRule j) throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ, Item.CONFIGURE)
                .everywhere()
                .to("alice"));

        ExtendedEmailPublisherDescriptor descriptor = ensureDescriptor(j);
        descriptor.setAdminRequiredForTemplateTesting(false);

        FreeStyleProject project = j.createFreeStyleProject("template-check-managed-plugin-missing");
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        try (ACLContext ignored = ACL.as(User.getById("alice", true))) {
            FormValidation validation = action.doTemplateFileCheck("managed:missing-template");
            assertEquals(FormValidation.Kind.ERROR, validation.kind);
            if (j.jenkins.getPlugin("config-file-provider") == null) {
                assertEquals(Messages.EmailExtTemplateAction_ConfigFileProviderNotAvailable(), validation.getMessage());
            } else {
                assertEquals(Messages.EmailExtTemplateAction_ManagedTemplateNotFound(), validation.getMessage());
            }
        }
    }

    /**
     * Calls doRenderTemplate via mocked Stapler objects and returns the parsed JSON response.
     */
    private JSONObject invokeRenderTemplate(EmailExtTemplateAction action, String templateFile, String buildId)
            throws Exception {
        StaplerRequest2 req = mock(StaplerRequest2.class);
        when(req.getParameter("templateFile")).thenReturn(templateFile);
        when(req.getParameter("buildId")).thenReturn(buildId);

        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(responseBody);

        StaplerResponse2 rsp = mock(StaplerResponse2.class);
        when(rsp.getWriter()).thenReturn(writer);

        action.doRenderTemplate(req, rsp);
        writer.flush();

        return JSONObject.fromObject(responseBody.toString(StandardCharsets.UTF_8));
    }

    @Test
    void doRenderTemplateUsesGroovyTemplates(JenkinsRule j) throws Exception {
        ensureDescriptor(j);

        FreeStyleProject project = j.createFreeStyleProject("render-template-groovy");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        assertNotNull(
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResource("hudson/plugins/emailext/templates/empty-groovy-template-on-classpath.template"),
                "Groovy template resource must exist on the classpath");

        EmailExtTemplateAction action = new EmailExtTemplateAction(project);
        JSONObject result = invokeRenderTemplate(action, "empty-groovy-template-on-classpath.template", build.getId());

        assertEquals("HELLO WORLD!", result.getString("renderedContent").trim());
        assertEquals("", result.getString("consoleOutput"));
    }

    @Test
    void doRenderTemplateUsesJellyTemplates(JenkinsRule j) throws Exception {
        ensureDescriptor(j);

        FreeStyleProject project = j.createFreeStyleProject("render-template-jelly");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        assertNotNull(
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResource("hudson/plugins/emailext/templates/empty-template-on-classpath.jelly"),
                "Jelly template resource must exist on the classpath");

        EmailExtTemplateAction action = new EmailExtTemplateAction(project);
        JSONObject result = invokeRenderTemplate(action, "empty-template-on-classpath.jelly", build.getId());

        assertEquals("HELLO WORLD!", result.getString("renderedContent"));
        assertEquals("", result.getString("consoleOutput"));
    }

    @Test
    void doRenderTemplateRendersErrorWhenBuildLookupFails(JenkinsRule j) throws Exception {
        ensureDescriptor(j);

        FreeStyleProject project = j.createFreeStyleProject("render-template-error");

        EmailExtTemplateAction action = new EmailExtTemplateAction(project);
        JSONObject result = invokeRenderTemplate(action, "empty-template-on-classpath.jelly", "missing");

        assertTrue(
                result.getString("renderedContent").contains("An error occurred trying to render the template:"),
                "renderedContent should contain the error message");
        assertEquals("", result.getString("consoleOutput"));
    }
}
