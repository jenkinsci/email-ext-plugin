package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import java.util.UUID;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

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

    @Test
    void renderTemplateUsesGroovyTemplates(JenkinsRule j) throws Exception {
        ensureDescriptor(j);

        FreeStyleProject project = j.createFreeStyleProject("render-template-groovy");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        assertNotNull(Thread.currentThread()
                .getContextClassLoader()
                .getResource("hudson/plugins/emailext/templates/empty-groovy-template-on-classpath.template"));

        EmailExtTemplateAction action = new EmailExtTemplateAction(project);
        String[] result = action.renderTemplate("empty-groovy-template-on-classpath.template", build.getId());

        assertEquals("HELLO WORLD!", result[0].trim());
        assertEquals("", result[1]);
    }

    @Test
    void renderTemplateUsesJellyTemplates(JenkinsRule j) throws Exception {
        ensureDescriptor(j);

        FreeStyleProject project = j.createFreeStyleProject("render-template-jelly");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        assertNotNull(Thread.currentThread()
                .getContextClassLoader()
                .getResource("hudson/plugins/emailext/templates/empty-template-on-classpath.jelly"));

        EmailExtTemplateAction action = new EmailExtTemplateAction(project);
        String[] result = action.renderTemplate("empty-template-on-classpath.jelly", build.getId());

        assertEquals("HELLO WORLD!", result[0]);
        assertEquals("", result[1]);
    }

    @Test
    void renderTemplateRendersErrorWhenBuildLookupFails(JenkinsRule j) throws Exception {
        ensureDescriptor(j);

        FreeStyleProject project = j.createFreeStyleProject("render-template-error");

        EmailExtTemplateAction action = new EmailExtTemplateAction(project);
        String[] result = action.renderTemplate("empty-template-on-classpath.jelly", "missing");

        assertTrue(result[0].contains("An error occurred trying to render the template:"));
        assertEquals("", result[1]);
    }
}
