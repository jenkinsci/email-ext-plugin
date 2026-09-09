package hudson.plugins.emailext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.jvnet.hudson.test.JenkinsMatchers.hasKind;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.plugins.trigger.AbortedTrigger;
import hudson.plugins.emailext.plugins.trigger.AlwaysTrigger;
import hudson.plugins.emailext.plugins.trigger.BuildingTrigger;
import hudson.plugins.emailext.plugins.trigger.FirstFailureTrigger;
import hudson.plugins.emailext.plugins.trigger.FirstUnstableTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedUnhealthyTrigger;
import hudson.plugins.emailext.plugins.trigger.ImprovementTrigger;
import hudson.plugins.emailext.plugins.trigger.NotBuiltTrigger;
import hudson.plugins.emailext.plugins.trigger.PreBuildScriptTrigger;
import hudson.plugins.emailext.plugins.trigger.PreBuildTrigger;
import hudson.plugins.emailext.plugins.trigger.RegressionTrigger;
import hudson.plugins.emailext.plugins.trigger.ScriptTrigger;
import hudson.plugins.emailext.plugins.trigger.SecondFailureTrigger;
import hudson.plugins.emailext.plugins.trigger.StatusChangedTrigger;
import hudson.plugins.emailext.plugins.trigger.StillFailingTrigger;
import hudson.plugins.emailext.plugins.trigger.StillUnstableTrigger;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import hudson.plugins.emailext.plugins.trigger.UnstableTrigger;
import hudson.plugins.emailext.plugins.trigger.XNthFailureTrigger;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import jakarta.mail.Authenticator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@WithJenkins
class ExtendedEmailPublisherDescriptorTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @Test
    @Issue("JENKINS-53898")
    void testDefaultAttachBuildLogDefaultValue() {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        assertEquals(
                0,
                descriptor.getDefaultAttachBuildLog(),
                "Default attach build log should be 0 (Do Not Attach) by default");
    }

    @Test
    void managePermissionShouldAccess() {
        final String USER = "user";
        final String MANAGER = "manager";
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                // Read access
                .grant(Jenkins.READ)
                .everywhere()
                .to(USER)

                // Read and Manage
                .grant(Jenkins.READ)
                .everywhere()
                .to(MANAGER)
                .grant(Jenkins.MANAGE)
                .everywhere()
                .to(MANAGER));
        try (ACLContext c = ACL.as(User.getById(USER, true))) {
            Collection<Descriptor> descriptors = Functions.getSortedDescriptorsForGlobalConfigUnclassified();
            assertEquals(0, descriptors.size(), "Global configuration should not be accessible to READ users");
        }
        try (ACLContext c = ACL.as(User.getById(MANAGER, true))) {
            Collection<Descriptor> descriptors =
                    Functions.getSortedDescriptorsForGlobalConfigByDescriptor(ExtendedEmailManagementLink.FILTER);
            Optional<Descriptor> found = descriptors.stream()
                    .filter(descriptor -> descriptor instanceof ExtendedEmailGlobalConfiguration)
                    .findFirst();
            assertTrue(found.isPresent(), "Global configuration should be accessible to MANAGE users");
        }
    }

    @Test
    void noAuthenticatorIsCreatedWhenCredentialsIsBlank() {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

        String from = "test@example.com";
        MailAccount ma = new MailAccount();
        ma.setAddress(from);
        ma.setSmtpHost("smtp.example.com");
        ma.setSmtpPort("25");
        ma.setCredentialsId(null);

        ExtendedEmailPublisher publisher = Mockito.mock(ExtendedEmailPublisher.class);
        Run<?, ?> run = Mockito.mock(Run.class);
        FilePath workspace = Mockito.mock(FilePath.class);
        Launcher launcher = Mockito.mock(Launcher.class);
        TaskListener listener = Mockito.mock(TaskListener.class);

        ExtendedEmailPublisherContext context =
                new ExtendedEmailPublisherContext(publisher, run, workspace, launcher, listener);

        descriptor.setAddAccounts(Collections.singletonList(ma));
        BiFunction<MailAccount, Run<?, ?>, Authenticator> authenticatorProvider = Mockito.mock(BiFunction.class);
        descriptor.setAuthenticatorProvider(authenticatorProvider);
        descriptor.createSession(ma, context);
        ArgumentCaptor<MailAccount> mailAccountCaptor = ArgumentCaptor.forClass(MailAccount.class);
        ArgumentCaptor<Run<?, ?>> runCaptor = ArgumentCaptor.forClass(Run.class);
        Mockito.verify(authenticatorProvider, Mockito.never()).apply(mailAccountCaptor.capture(), runCaptor.capture());
    }

    @Test
    void authenticatorIsCreatedWhenCredentialsIdProvided() throws Exception {
        UsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "email-ext-admin", "Username/password for SMTP", "admin", "honeycomb");
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), c);
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

        String from = "test@example.com";
        MailAccount ma = new MailAccount();
        ma.setAddress(from);
        ma.setSmtpHost("smtp.example.com");
        ma.setSmtpPort("25");
        ma.setCredentialsId("email-ext-admin");

        ExtendedEmailPublisher publisher = Mockito.mock(ExtendedEmailPublisher.class);
        Run<?, ?> run = Mockito.mock(Run.class);
        FilePath workspace = Mockito.mock(FilePath.class);
        Launcher launcher = Mockito.mock(Launcher.class);
        TaskListener listener = Mockito.mock(TaskListener.class);

        ExtendedEmailPublisherContext context =
                new ExtendedEmailPublisherContext(publisher, run, workspace, launcher, listener);

        BiFunction<MailAccount, Run<?, ?>, Authenticator> authenticatorProvider = Mockito.mock(BiFunction.class);
        descriptor.setAuthenticatorProvider(authenticatorProvider);
        descriptor.createSession(ma, context);
        ArgumentCaptor<MailAccount> mailAccountCaptor = ArgumentCaptor.forClass(MailAccount.class);
        ArgumentCaptor<Run<?, ?>> runCaptor = ArgumentCaptor.forClass(Run.class);
        Mockito.verify(authenticatorProvider, Mockito.atLeast(1))
                .apply(mailAccountCaptor.capture(), runCaptor.capture());
    }

    @LocalData
    @Test
    void persistedConfigurationBeforeJCasC() {
        // Local data created using Email Extension 2.71 with the following code:
        /*
         * HtmlPage page = j.createWebClient().goTo("configure");
         * HtmlForm config = page.getFormByName("config");
         * j.submit(config);
         *
         * page = j.createWebClient().goTo("configure");
         * config = page.getFormByName("config");
         * j.getButtonByCaption(config, "Advanced...").click();
         * j.getButtonByCaption(config, "Default Triggers...").click();
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * HtmlCheckBoxInput useSmtpAuth =
         * page.getElementByName("ext_mailer_use_smtp_auth");
         * useSmtpAuth.click();
         * HtmlCheckBoxInput useListId =
         * page.getElementByName("ext_mailer_use_list_id");
         * useListId.click();
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * Set<HtmlButton> buttons = new HashSet<>();
         * for (HtmlElement button : config.getElementsByTagName("button")) {
         * buttons.add((HtmlButton) button);
         * }
         * for (HtmlButton button : buttons) {
         * DomNode ancestor =
         * button.getParentNode().getParentNode().getParentNode().getParentNode();
         * String key = ancestor.getPreviousSibling().getTextContent().trim();
         * if (key.equals("Additional accounts") ||
         * key.equals("Additional groovy classpath")) {
         * button.click();
         * }
         * }
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * HtmlCheckBoxInput useSmtpAuth2 = page.getElementByName("_.auth");
         * useSmtpAuth2.click();
         * WebClientUtil.waitForJSExec(page.getWebClient());
         *
         * HtmlTextInput address = page.getElementByName("_.adminAddress");
         * address.setValue("admin@example.com");
         * HtmlTextInput smtpServer = page.getElementByName("ext_mailer_smtp_server");
         * smtpServer.setValue("smtp.example.com");
         * HtmlTextInput defaultSuffix =
         * page.getElementByName("ext_mailer_default_suffix");
         * defaultSuffix.setValue("@example.com");
         * HtmlTextInput smtpUsername =
         * page.getElementByName("ext_mailer_smtp_username");
         * smtpUsername.setValue("admin");
         * HtmlPasswordInput smtpPassword =
         * page.getElementByName("ext_mailer_smtp_password");
         * smtpPassword.setValue("honeycomb");
         * HtmlTextArea advProperties =
         * page.getElementByName("ext_mailer_adv_properties");
         * advProperties.setText("mail.smtp.ssl.trust=example.com");
         * HtmlCheckBoxInput smtpUseSsl =
         * page.getElementByName("ext_mailer_smtp_use_ssl");
         * smtpUseSsl.click();
         * HtmlTextInput smtpPort = page.getElementByName("ext_mailer_smtp_port");
         * smtpPort.setValue("2525");
         * HtmlTextInput charset = page.getElementByName("ext_mailer_charset");
         * charset.setValue("UTF-8");
         * HtmlTextInput address2 = page.getElementByName("_.address");
         * address2.setValue("admin@example2.com");
         * HtmlTextInput smtpServer2 = page.getElementByName("_.smtpHost");
         * smtpServer2.setValue("smtp.example2.com");
         * HtmlTextInput smtpPort2 = page.getElementByName("_.smtpPort");
         * smtpPort2.setValue("2626");
         * HtmlTextInput smtpUsername2 = page.getElementByName("_.smtpUsername");
         * smtpUsername2.setValue("admin2");
         * HtmlPasswordInput smtpPassword2 = page.getElementByName("_.smtpPassword");
         * smtpPassword2.setValue("honeycomb2");
         * HtmlCheckBoxInput smtpUseSsl2 = page.getElementByName("_.useSsl");
         * smtpUseSsl2.click();
         * HtmlTextArea advProperties2 = page.getElementByName("_.advProperties");
         * advProperties2.setText("mail.smtp.ssl.trust=example2.com");
         * HtmlSelect defaultContentType =
         * page.getElementByName("ext_mailer_default_content_type");
         * defaultContentType.setSelectedAttribute("text/html", true);
         * HtmlTextInput listId = page.getElementByName("ext_mailer_list_id");
         * listId.setValue("<list.example.com>");
         * HtmlCheckBoxInput addPrecedenceBulk =
         * page.getElementByName("ext_mailer_add_precedence_bulk");
         * addPrecedenceBulk.click();
         * HtmlTextInput defaultRecipients =
         * page.getElementByName("ext_mailer_default_recipients");
         * defaultRecipients.setValue("default@example.com");
         * HtmlTextInput defaultReplyto =
         * page.getElementByName("ext_mailer_default_replyto");
         * defaultReplyto.setValue("noreply@example.com");
         * HtmlTextInput emergencyReroute =
         * page.getElementByName("ext_mailer_emergency_reroute");
         * emergencyReroute.setValue("emergency@example.com");
         * HtmlTextInput allowedDomains =
         * page.getElementByName("ext_mailer_allowed_domains");
         * allowedDomains.setValue("@example.com");
         * HtmlTextInput excludedCommitters =
         * page.getElementByName("ext_mailer_excluded_committers");
         * excludedCommitters.setValue("excluded@example.com");
         * HtmlTextInput defaultSubject =
         * page.getElementByName("ext_mailer_default_subject");
         * defaultSubject.
         * setValue("$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS");
         * HtmlTextInput maxAttachmentSize =
         * page.getElementByName("ext_mailer_max_attachment_size");
         * maxAttachmentSize.setValue("42");
         * HtmlTextArea defaultBody = page.getElementByName("ext_mailer_default_body");
         * defaultBody.setText("$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS");
         * HtmlTextArea defaultPresendScript =
         * page.getElementByName("ext_mailer_default_presend_script");
         * defaultPresendScript.setText(
         * "build.previousBuild.result.toString().equals('FAILURE')");
         * HtmlTextArea defaultPostsendScript =
         * page.getElementByName("ext_mailer_default_postsend_script");
         * defaultPostsendScript.setText("build.result.toString().equals('FAILURE')");
         * HtmlTextInput defaultClasspath =
         * page.getElementByName("ext_mailer_default_classpath");
         * defaultClasspath.setValue("classes");
         * HtmlCheckBoxInput debugMode = page.getElementByName("ext_mailer_debug_mode");
         * debugMode.click();
         * HtmlCheckBoxInput requireAdminForTemplateTesting =
         * page.getElementByName("ext_mailer_require_admin_for_template_testing");
         * requireAdminForTemplateTesting.click();
         * HtmlCheckBoxInput watchingEnabled =
         * page.getElementByName("ext_mailer_watching_enabled");
         * watchingEnabled.click();
         * HtmlCheckBoxInput allowUnregisteredEnabled =
         * page.getElementByName("ext_mailer_allow_unregistered_enabled");
         * allowUnregisteredEnabled.click();
         * for (HtmlInput input : config.getInputsByName("defaultTriggers")) {
         * input.click();
         * }
         *
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * j.submit(config);
         */
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        assertEquals("admin@example.com", descriptor.getAdminAddress());
        assertEquals("smtp.example.com", descriptor.getMailAccount().getSmtpHost());
        assertEquals("@example.com", descriptor.getDefaultSuffix());
        assertNotNull(descriptor.getMailAccount().getCredentialsId());
        List<StandardCredentials> creds = CredentialsProvider.lookupCredentials(StandardCredentials.class);
        assertEquals(2, creds.size());
        for (StandardCredentials c : creds) {
            assertEquals(UsernamePasswordCredentialsImpl.class, c.getClass());
            assertEquals("Migrated from email-ext username/password", c.getDescription());
        }
        assertEquals(
                "mail.smtp.ssl.trust=example.com", descriptor.getMailAccount().getAdvProperties());
        assertTrue(descriptor.getMailAccount().isUseSsl());
        assertTrue(descriptor.getMailAccount().isDefaultAccount());
        assertEquals("2525", descriptor.getMailAccount().getSmtpPort());
        assertEquals("UTF-8", descriptor.getCharset());
        assertEquals(1, descriptor.getAddAccounts().size());
        MailAccount additionalAccount = descriptor.getAddAccounts().get(0);
        assertEquals("admin@example2.com", additionalAccount.getAddress());
        assertEquals("smtp.example2.com", additionalAccount.getSmtpHost());
        assertEquals("2626", additionalAccount.getSmtpPort());
        assertNotNull(additionalAccount.getCredentialsId());
        assertTrue(additionalAccount.isUseSsl());
        assertFalse(additionalAccount.isDefaultAccount());
        assertEquals("mail.smtp.ssl.trust=example2.com", additionalAccount.getAdvProperties());
        assertEquals("text/html", descriptor.getDefaultContentType());
        assertEquals("<list.example.com>", descriptor.getListId());
        assertTrue(descriptor.getPrecedenceBulk());
        assertEquals("default@example.com", descriptor.getDefaultRecipients());
        assertEquals("noreply@example.com", descriptor.getDefaultReplyTo());
        assertEquals("emergency@example.com", descriptor.getEmergencyReroute());
        assertEquals("@example.com", descriptor.getAllowedDomains());
        assertEquals("excluded@example.com", descriptor.getExcludedCommitters());
        assertEquals("$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS", descriptor.getDefaultSubject());
        assertEquals(44040192, descriptor.getMaxAttachmentSize());
        assertEquals("$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS", descriptor.getDefaultBody());
        assertEquals("build.previousBuild.result.toString().equals('FAILURE')", descriptor.getDefaultPresendScript());
        assertEquals("build.result.toString().equals('FAILURE')", descriptor.getDefaultPostsendScript());
        assertEquals(1, descriptor.getDefaultClasspath().size());
        assertEquals("classes", descriptor.getDefaultClasspath().get(0).getPath());
        assertTrue(descriptor.isDebugMode());
        assertTrue(descriptor.isAdminRequiredForTemplateTesting());
        assertTrue(descriptor.isWatchingEnabled());
        assertTrue(descriptor.isAllowUnregisteredEnabled());
        assertEquals(20, descriptor.getDefaultTriggerIds().size());
        assertThat(
                descriptor.getDefaultTriggerIds(),
                containsInAnyOrder(
                        AbortedTrigger.class.getName(),
                        AlwaysTrigger.class.getName(),
                        BuildingTrigger.class.getName(),
                        FirstFailureTrigger.class.getName(),
                        FirstUnstableTrigger.class.getName(),
                        FixedTrigger.class.getName(),
                        FixedUnhealthyTrigger.class.getName(),
                        ImprovementTrigger.class.getName(),
                        NotBuiltTrigger.class.getName(),
                        PreBuildScriptTrigger.class.getName(),
                        PreBuildTrigger.class.getName(),
                        RegressionTrigger.class.getName(),
                        ScriptTrigger.class.getName(),
                        SecondFailureTrigger.class.getName(),
                        StatusChangedTrigger.class.getName(),
                        StillFailingTrigger.class.getName(),
                        StillUnstableTrigger.class.getName(),
                        SuccessTrigger.class.getName(),
                        UnstableTrigger.class.getName(),
                        XNthFailureTrigger.class.getName()));
    }

    @Issue("JENKINS-63846")
    @LocalData
    @Test
    void persistedConfigurationBeforeDefaultAddress() {
        // Local data created using Email Extension 2.72 with the following code:
        /*
         * HtmlPage page = j.createWebClient().goTo("configure");
         * HtmlForm config = page.getFormByName("config");
         * j.submit(config);
         *
         * page = j.createWebClient().goTo("configure");
         * config = page.getFormByName("config");
         * for (HtmlElement button : config.getElementsByTagName("button")) {
         * if (button.getTextContent().trim().equals("Advanced...")) {
         * button.click();
         * }
         * }
         * j.getButtonByCaption(config, "Default Triggers...").click();
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * Set<HtmlButton> buttons = new HashSet<>();
         * for (HtmlElement button : config.getElementsByTagName("button")) {
         * buttons.add((HtmlButton) button);
         * }
         * for (HtmlButton button : buttons) {
         * DomNode ancestor =
         * button.getParentNode().getParentNode().getParentNode().getParentNode();
         * String key = ancestor.getPreviousSibling().getTextContent().trim();
         * if (key.equals("Additional accounts") ||
         * key.equals("Additional groovy classpath")) {
         * button.click();
         * }
         * }
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * buttons = new HashSet<>();
         * for (HtmlElement button : config.getElementsByTagName("button")) {
         * buttons.add((HtmlButton) button);
         * }
         * for (HtmlButton button : buttons) {
         * String textContent = button.getTextContent().trim();
         * if (textContent.equals("Advanced...")) {
         * DomNode ancestor =
         * button.getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode();
         * String key = ancestor.getPreviousSibling().getTextContent().trim();
         * if (key.equals("Additional accounts")) {
         * button.click();
         * }
         * }
         * }
         * WebClientUtil.waitForJSExec(page.getWebClient());
         *
         * HtmlTextInput address = page.getElementByName("_.adminAddress");
         * address.setValue("admin@example.com");
         * List<DomElement> smtpServers = page.getElementsByName("_.smtpHost");
         * HtmlTextInput smtpServer = (HtmlTextInput) smtpServers.get(0);
         * smtpServer.setValue("smtp.example.com");
         * List<DomElement> smtpPorts = page.getElementsByName("_.smtpPort");
         * HtmlNumberInput smtpPort = (HtmlNumberInput) smtpPorts.get(0);
         * smtpPort.setValue("2525");
         * List<DomElement> smtpUsernames = page.getElementsByName("_.smtpUsername");
         * HtmlTextInput smtpUsername = (HtmlTextInput) smtpUsernames.get(0);
         * smtpUsername.setValue("admin");
         * List<DomElement> smtpPasswords = page.getElementsByName("_.smtpPassword");
         * HtmlTextInput smtpPassword = (HtmlTextInput) smtpPasswords.get(0);
         * smtpPassword.setValue("honeycomb");
         * List<DomElement> smtpUseSsls = page.getElementsByName("_.useSsl");
         * HtmlCheckBoxInput smtpUseSsl = (HtmlCheckBoxInput) smtpUseSsls.get(0);
         * smtpUseSsl.click();
         * List<DomElement> advPropertiesElements =
         * page.getElementsByName("_.advProperties");
         * HtmlTextArea advProperties = (HtmlTextArea) advPropertiesElements.get(0);
         * advProperties.setText("mail.smtp.ssl.trust=example.com");
         * HtmlTextInput defaultSuffix = page.getElementByName("_.defaultSuffix");
         * defaultSuffix.setValue("@example.com");
         * HtmlTextInput charset = page.getElementByName("_.charset");
         * charset.setValue("UTF-8");
         * List<DomElement> addresses = page.getElementsByName("_.address");
         * HtmlTextInput address2 = (HtmlTextInput) addresses.get(1);
         * address2.setValue("admin@example2.com");
         * HtmlTextInput smtpServer2 = (HtmlTextInput) smtpServers.get(1);
         * smtpServer2.setValue("smtp.example2.com");
         * HtmlNumberInput smtpPort2 = (HtmlNumberInput) smtpPorts.get(1);
         * smtpPort2.setValue("2626");
         * HtmlTextInput smtpUsername2 = (HtmlTextInput) smtpUsernames.get(1);
         * smtpUsername2.setValue("admin2");
         * HtmlTextInput smtpPassword2 = (HtmlTextInput) smtpPasswords.get(1);
         * smtpPassword2.setValue("honeycomb2");
         * HtmlCheckBoxInput smtpUseSsl2 = (HtmlCheckBoxInput) smtpUseSsls.get(1);
         * smtpUseSsl2.click();
         * HtmlTextArea advProperties2 = (HtmlTextArea) advPropertiesElements.get(1);
         * advProperties2.setText("mail.smtp.ssl.trust=example2.com");
         * HtmlSelect defaultContentType =
         * page.getElementByName("_.defaultContentType");
         * defaultContentType.setSelectedAttribute("text/html", true);
         * HtmlTextInput listId = page.getElementByName("_.listId");
         * listId.setValue("<list.example.com>");
         * HtmlCheckBoxInput addPrecedenceBulk =
         * page.getElementByName("_.precedenceBulk");
         * addPrecedenceBulk.click();
         * HtmlTextInput defaultRecipients =
         * page.getElementByName("_.defaultRecipients");
         * defaultRecipients.setValue("default@example.com");
         * HtmlTextInput defaultReplyto = page.getElementByName("_.defaultReplyTo");
         * defaultReplyto.setValue("noreply@example.com");
         * HtmlTextInput emergencyReroute = page.getElementByName("_.emergencyReroute");
         * emergencyReroute.setValue("emergency@example.com");
         * HtmlTextInput allowedDomains = page.getElementByName("_.allowedDomains");
         * allowedDomains.setValue("@example.com");
         * HtmlTextInput excludedCommitters =
         * page.getElementByName("_.excludedCommitters");
         * excludedCommitters.setValue("excluded@example.com");
         * HtmlTextInput defaultSubject = page.getElementByName("_.defaultSubject");
         * defaultSubject.
         * setValue("$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS");
         * HtmlNumberInput maxAttachmentSize =
         * page.getElementByName("_.maxAttachmentSizeMb");
         * maxAttachmentSize.setValue("42");
         * HtmlTextArea defaultBody = page.getElementByName("_.defaultBody");
         * defaultBody.setText("$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS");
         * HtmlTextArea defaultPresendScript =
         * page.getElementByName("_.defaultPresendScript");
         * defaultPresendScript.setText(
         * "build.previousBuild.result.toString().equals('FAILURE')");
         * HtmlTextArea defaultPostsendScript =
         * page.getElementByName("_.defaultPostsendScript");
         * defaultPostsendScript.setText("build.result.toString().equals('FAILURE')");
         * HtmlTextInput defaultClasspath = page.getElementByName("_.path");
         * defaultClasspath.setValue("classes");
         * HtmlCheckBoxInput debugMode = page.getElementByName("_.debugMode");
         * debugMode.click();
         * HtmlCheckBoxInput requireAdminForTemplateTesting =
         * page.getElementByName("_.adminRequiredForTemplateTesting");
         * requireAdminForTemplateTesting.click();
         * HtmlCheckBoxInput watchingEnabled =
         * page.getElementByName("_.watchingEnabled");
         * watchingEnabled.click();
         * HtmlCheckBoxInput allowUnregisteredEnabled =
         * page.getElementByName("_.allowUnregisteredEnabled");
         * allowUnregisteredEnabled.click();
         * for (HtmlInput input : config.getInputsByName("_.defaultTriggerIds")) {
         * input.click();
         * }
         *
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * j.submit(config);
         */
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        assertEquals("admin@example.com", descriptor.getAdminAddress());
        assertNull(descriptor.getMailAccount().getAddress());
        assertEquals("smtp.example.com", descriptor.getMailAccount().getSmtpHost());
        assertEquals("@example.com", descriptor.getDefaultSuffix());
        assertNotNull(descriptor.getMailAccount().getCredentialsId());
        List<StandardCredentials> creds = CredentialsProvider.lookupCredentials(StandardCredentials.class);
        assertEquals(2, creds.size());
        for (StandardCredentials c : creds) {
            assertEquals(UsernamePasswordCredentialsImpl.class, c.getClass());
            assertEquals("Migrated from email-ext username/password", c.getDescription());
        }
        assertEquals(
                "mail.smtp.ssl.trust=example.com", descriptor.getMailAccount().getAdvProperties());
        assertTrue(descriptor.getMailAccount().isUseSsl());
        assertTrue(descriptor.getMailAccount().isDefaultAccount());
        assertEquals("2525", descriptor.getMailAccount().getSmtpPort());
        assertEquals("UTF-8", descriptor.getCharset());
        assertEquals(1, descriptor.getAddAccounts().size());
        MailAccount additionalAccount = descriptor.getAddAccounts().get(0);
        assertEquals("admin@example2.com", additionalAccount.getAddress());
        assertEquals("smtp.example2.com", additionalAccount.getSmtpHost());
        assertEquals("2626", additionalAccount.getSmtpPort());
        assertNotNull(additionalAccount.getCredentialsId());
        assertTrue(additionalAccount.isUseSsl());
        assertFalse(additionalAccount.isDefaultAccount());
        assertEquals("mail.smtp.ssl.trust=example2.com", additionalAccount.getAdvProperties());
        assertEquals("text/html", descriptor.getDefaultContentType());
        assertEquals("<list.example.com>", descriptor.getListId());
        assertTrue(descriptor.getPrecedenceBulk());
        assertEquals("default@example.com", descriptor.getDefaultRecipients());
        assertEquals("noreply@example.com", descriptor.getDefaultReplyTo());
        assertEquals("emergency@example.com", descriptor.getEmergencyReroute());
        assertEquals("@example.com", descriptor.getAllowedDomains());
        assertEquals("excluded@example.com", descriptor.getExcludedCommitters());
        assertEquals("$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS", descriptor.getDefaultSubject());
        assertEquals(44040192, descriptor.getMaxAttachmentSize());
        assertEquals("$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS", descriptor.getDefaultBody());
        assertEquals("build.previousBuild.result.toString().equals('FAILURE')", descriptor.getDefaultPresendScript());
        assertEquals("build.result.toString().equals('FAILURE')", descriptor.getDefaultPostsendScript());
        assertEquals(1, descriptor.getDefaultClasspath().size());
        assertEquals("classes", descriptor.getDefaultClasspath().get(0).getPath());
        assertTrue(descriptor.isDebugMode());
        assertTrue(descriptor.isAdminRequiredForTemplateTesting());
        assertTrue(descriptor.isWatchingEnabled());
        assertTrue(descriptor.isAllowUnregisteredEnabled());
        assertEquals(20, descriptor.getDefaultTriggerIds().size());
        assertThat(
                descriptor.getDefaultTriggerIds(),
                containsInAnyOrder(
                        AbortedTrigger.class.getName(),
                        AlwaysTrigger.class.getName(),
                        BuildingTrigger.class.getName(),
                        FirstFailureTrigger.class.getName(),
                        FirstUnstableTrigger.class.getName(),
                        FixedTrigger.class.getName(),
                        FixedUnhealthyTrigger.class.getName(),
                        ImprovementTrigger.class.getName(),
                        NotBuiltTrigger.class.getName(),
                        PreBuildScriptTrigger.class.getName(),
                        PreBuildTrigger.class.getName(),
                        RegressionTrigger.class.getName(),
                        ScriptTrigger.class.getName(),
                        SecondFailureTrigger.class.getName(),
                        StatusChangedTrigger.class.getName(),
                        StillFailingTrigger.class.getName(),
                        StillUnstableTrigger.class.getName(),
                        SuccessTrigger.class.getName(),
                        UnstableTrigger.class.getName(),
                        XNthFailureTrigger.class.getName()));
    }

    @Test
    @LocalData
    void persistedConfigurationWithCredentialId() {
        // Local data created using Email Extension 2.72 with the following code:
        /*
         * // add two credentials to the GLOBAL scope
         * UsernamePasswordCredentials c1 = new
         * UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "email-ext-admin",
         * "Username/password for SMTP", "admin", "honeycomb");
         * UsernamePasswordCredentials c2 = new
         * UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "email-ext-admin2",
         * "Username/password for SMTP", "admin2", "honeycomb2");
         * CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(
         * Domain.global(), c1);
         * CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(
         * Domain.global(), c2);
         *
         * HtmlPage page = j.createWebClient().goTo("configure");
         * HtmlForm config = page.getFormByName("config");
         * j.submit(config);
         *
         * page = j.createWebClient().goTo("configure");
         * config = page.getFormByName("config");
         * for (HtmlElement button : config.getElementsByTagName("button")) {
         * if (button.getTextContent().trim().equals("Advanced...")) {
         * button.click();
         * }
         * }
         * j.getButtonByCaption(config, "Default Triggers...").click();
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * Set<HtmlButton> buttons = new HashSet<>();
         * for (HtmlElement button : config.getElementsByTagName("button")) {
         * buttons.add((HtmlButton) button);
         * }
         * for (HtmlButton button : buttons) {
         * DomNode ancestor =
         * button.getParentNode().getParentNode().getParentNode().getParentNode();
         * if(ancestor.getPreviousSibling() == null) {
         * continue;
         * }
         * String key = ancestor.getPreviousSibling().getTextContent().trim();
         * if (key.equals("Additional accounts") ||
         * key.equals("Additional groovy classpath")) {
         * button.click();
         * }
         * }
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * buttons = new HashSet<>();
         * for (HtmlElement button : config.getElementsByTagName("button")) {
         * buttons.add((HtmlButton) button);
         * }
         * for (HtmlButton button : buttons) {
         * String textContent = button.getTextContent().trim();
         * if (textContent.equals("Advanced...")) {
         * DomNode ancestor =
         * button.getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode()
         * .getParentNode();
         * String key = ancestor.getPreviousSibling().getTextContent().trim();
         * if (key.equals("Additional accounts")) {
         * button.click();
         * }
         * }
         * }
         * WebClientUtil.waitForJSExec(page.getWebClient());
         *
         * HtmlTextInput address = page.getElementByName("_.adminAddress");
         * address.setValue("admin@example.com");
         * List<DomElement> smtpServers = page.getElementsByName("_.smtpHost");
         * HtmlTextInput smtpServer = (HtmlTextInput) smtpServers.get(0);
         * smtpServer.setValue("smtp.example.com");
         * List<DomElement> smtpPorts = page.getElementsByName("_.smtpPort");
         * HtmlNumberInput smtpPort = (HtmlNumberInput) smtpPorts.get(0);
         * smtpPort.setValue("2525");
         * List<DomElement> credentialsIds = page.getElementsByName("_.credentialsId");
         * HtmlSelect credentialsId = (HtmlSelect)credentialsIds.get(0);
         * List<HtmlOption> options = credentialsId.getOptions();
         * credentialsId.setSelectedIndex(1);
         * List<DomElement> smtpUseSsls = page.getElementsByName("_.useSsl");
         * HtmlCheckBoxInput smtpUseSsl = (HtmlCheckBoxInput) smtpUseSsls.get(0);
         * smtpUseSsl.click();
         * List<DomElement> advPropertiesElements =
         * page.getElementsByName("_.advProperties");
         * HtmlTextArea advProperties = (HtmlTextArea) advPropertiesElements.get(0);
         * advProperties.setText("mail.smtp.ssl.trust=example.com");
         * HtmlTextInput defaultSuffix = page.getElementByName("_.defaultSuffix");
         * defaultSuffix.setValue("@example.com");
         * HtmlTextInput charset = page.getElementByName("_.charset");
         * charset.setValue("UTF-8");
         * List<DomElement> addresses = page.getElementsByName("_.address");
         * HtmlTextInput address2 = (HtmlTextInput) addresses.get(1);
         * address2.setValue("admin@example2.com");
         * HtmlTextInput smtpServer2 = (HtmlTextInput) smtpServers.get(1);
         * smtpServer2.setValue("smtp.example2.com");
         * HtmlNumberInput smtpPort2 = (HtmlNumberInput) smtpPorts.get(1);
         * smtpPort2.setValue("2626");
         * HtmlSelect credentialId2 = (HtmlSelect)credentialsIds.get(1);
         * credentialId2.setSelectedIndex(2);
         * HtmlCheckBoxInput smtpUseSsl2 = (HtmlCheckBoxInput) smtpUseSsls.get(1);
         * smtpUseSsl2.click();
         * HtmlTextArea advProperties2 = (HtmlTextArea) advPropertiesElements.get(1);
         * advProperties2.setText("mail.smtp.ssl.trust=example2.com");
         * HtmlSelect defaultContentType =
         * page.getElementByName("_.defaultContentType");
         * defaultContentType.setSelectedAttribute("text/html", true);
         * HtmlTextInput listId = page.getElementByName("_.listId");
         * listId.setValue("<list.example.com>");
         * HtmlCheckBoxInput addPrecedenceBulk =
         * page.getElementByName("_.precedenceBulk");
         * addPrecedenceBulk.click();
         * HtmlTextInput defaultRecipients =
         * page.getElementByName("_.defaultRecipients");
         * defaultRecipients.setValue("default@example.com");
         * HtmlTextInput defaultReplyto = page.getElementByName("_.defaultReplyTo");
         * defaultReplyto.setValue("noreply@example.com");
         * HtmlTextInput emergencyReroute = page.getElementByName("_.emergencyReroute");
         * emergencyReroute.setValue("emergency@example.com");
         * HtmlTextInput allowedDomains = page.getElementByName("_.allowedDomains");
         * allowedDomains.setValue("@example.com");
         * HtmlTextInput excludedCommitters =
         * page.getElementByName("_.excludedCommitters");
         * excludedCommitters.setValue("excluded@example.com");
         * HtmlTextInput defaultSubject = page.getElementByName("_.defaultSubject");
         * defaultSubject.
         * setValue("$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS");
         * HtmlNumberInput maxAttachmentSize =
         * page.getElementByName("_.maxAttachmentSizeMb");
         * maxAttachmentSize.setValue("42");
         * HtmlTextArea defaultBody = page.getElementByName("_.defaultBody");
         * defaultBody.setText("$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS");
         * HtmlTextArea defaultPresendScript =
         * page.getElementByName("_.defaultPresendScript");
         * defaultPresendScript.setText(
         * "build.previousBuild.result.toString().equals('FAILURE')");
         * HtmlTextArea defaultPostsendScript =
         * page.getElementByName("_.defaultPostsendScript");
         * defaultPostsendScript.setText("build.result.toString().equals('FAILURE')");
         * HtmlTextInput defaultClasspath = page.getElementByName("_.path");
         * defaultClasspath.setValue("classes");
         * HtmlCheckBoxInput debugMode = page.getElementByName("_.debugMode");
         * debugMode.click();
         * HtmlCheckBoxInput requireAdminForTemplateTesting =
         * page.getElementByName("_.adminRequiredForTemplateTesting");
         * requireAdminForTemplateTesting.click();
         * HtmlCheckBoxInput watchingEnabled =
         * page.getElementByName("_.watchingEnabled");
         * watchingEnabled.click();
         * HtmlCheckBoxInput allowUnregisteredEnabled =
         * page.getElementByName("_.allowUnregisteredEnabled");
         * allowUnregisteredEnabled.click();
         * for (HtmlInput input : config.getInputsByName("_.defaultTriggerIds")) {
         * input.click();
         * }
         *
         * WebClientUtil.waitForJSExec(page.getWebClient());
         * j.submit(config);
         */

        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        assertEquals("admin@example.com", descriptor.getAdminAddress());
        assertNull(descriptor.getMailAccount().getAddress());
        assertEquals("smtp.example.com", descriptor.getMailAccount().getSmtpHost());
        assertEquals("@example.com", descriptor.getDefaultSuffix());
        assertEquals("email-ext-admin", descriptor.getMailAccount().getCredentialsId());
        assertEquals(
                "mail.smtp.ssl.trust=example.com", descriptor.getMailAccount().getAdvProperties());
        assertTrue(descriptor.getMailAccount().isUseSsl());
        assertTrue(descriptor.getMailAccount().isDefaultAccount());
        assertEquals("2525", descriptor.getMailAccount().getSmtpPort());
        assertEquals("UTF-8", descriptor.getCharset());
        assertEquals(1, descriptor.getAddAccounts().size());
        MailAccount additionalAccount = descriptor.getAddAccounts().get(0);
        assertEquals("admin@example2.com", additionalAccount.getAddress());
        assertEquals("smtp.example2.com", additionalAccount.getSmtpHost());
        assertEquals("2626", additionalAccount.getSmtpPort());
        assertEquals("email-ext-admin2", additionalAccount.getCredentialsId());
        assertTrue(additionalAccount.isUseSsl());
        assertFalse(additionalAccount.isDefaultAccount());
        assertEquals("mail.smtp.ssl.trust=example2.com", additionalAccount.getAdvProperties());
        assertEquals("text/html", descriptor.getDefaultContentType());
        assertEquals("<list.example.com>", descriptor.getListId());
        assertTrue(descriptor.getPrecedenceBulk());
        assertEquals("default@example.com", descriptor.getDefaultRecipients());
        assertEquals("noreply@example.com", descriptor.getDefaultReplyTo());
        assertEquals("emergency@example.com", descriptor.getEmergencyReroute());
        assertEquals("@example.com", descriptor.getAllowedDomains());
        assertEquals("excluded@example.com", descriptor.getExcludedCommitters());
        assertEquals("$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS", descriptor.getDefaultSubject());
        assertEquals(44040192, descriptor.getMaxAttachmentSize());
        assertEquals("$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS", descriptor.getDefaultBody());
        assertEquals("build.previousBuild.result.toString().equals('FAILURE')", descriptor.getDefaultPresendScript());
        assertEquals("build.result.toString().equals('FAILURE')", descriptor.getDefaultPostsendScript());
        assertEquals(1, descriptor.getDefaultClasspath().size());
        assertEquals("classes", descriptor.getDefaultClasspath().get(0).getPath());
        assertTrue(descriptor.isDebugMode());
        assertTrue(descriptor.isAdminRequiredForTemplateTesting());
        assertTrue(descriptor.isWatchingEnabled());
        assertTrue(descriptor.isAllowUnregisteredEnabled());
        assertEquals(20, descriptor.getDefaultTriggerIds().size());
        assertThat(
                descriptor.getDefaultTriggerIds(),
                containsInAnyOrder(
                        AbortedTrigger.class.getName(),
                        AlwaysTrigger.class.getName(),
                        BuildingTrigger.class.getName(),
                        FirstFailureTrigger.class.getName(),
                        FirstUnstableTrigger.class.getName(),
                        FixedTrigger.class.getName(),
                        FixedUnhealthyTrigger.class.getName(),
                        ImprovementTrigger.class.getName(),
                        NotBuiltTrigger.class.getName(),
                        PreBuildScriptTrigger.class.getName(),
                        PreBuildTrigger.class.getName(),
                        RegressionTrigger.class.getName(),
                        ScriptTrigger.class.getName(),
                        SecondFailureTrigger.class.getName(),
                        StatusChangedTrigger.class.getName(),
                        StillFailingTrigger.class.getName(),
                        StillUnstableTrigger.class.getName(),
                        SuccessTrigger.class.getName(),
                        UnstableTrigger.class.getName(),
                        XNthFailureTrigger.class.getName()));
    }

    @Test
    void emptyScriptApproval() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));
        j.submit(j.createWebClient().login("admin").goTo("configure").getFormByName("config"));
        assertThat(ScriptApproval.get().getPendingScripts(), is(empty()));
    }

    @Test
    @Issue("JENKINS-41473")
    void testBothContentTypeAvailableInGlobalConfig() {
        ExtendedEmailGlobalConfiguration globalConfig = ExtendedEmailGlobalConfiguration.get();

        ListBoxModel items = globalConfig.doFillDefaultContentTypeItems();

        assertEquals(3, items.size(), "Should have exactly three content type options");

        List<String> values = items.stream().map(option -> option.value).toList();
        assertTrue(values.contains("text/plain"), "Should have plain text option");
        assertTrue(values.contains("text/html"), "Should have HTML option");
        assertTrue(values.contains("both"), "Should have 'both' option");
    }

    @Test
    @Issue("JENKINS-41473")
    void testGlobalBothContentTypeCanBeSetAndInherited() throws Exception {
        ExtendedEmailGlobalConfiguration globalConfig = ExtendedEmailGlobalConfiguration.get();
        globalConfig.setDefaultContentType("both");
        globalConfig.save();

        assertEquals("both", globalConfig.getDefaultContentType(), "Global content type should be set to 'both'");

        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        assertEquals(
                "both", descriptor.getDefaultContentType(), "'both' content type should persist after save and reload");
    }

    @Test
    void testAttachmentsPatternValidation() {
        ExtendedEmailPublisherDescriptor desc = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

        // Empty pattern – warning
        assertThat(desc.doCheckAttachmentsPattern(""), hasKind(Kind.WARNING));

        // Valid patterns – OK
        assertThat(desc.doCheckAttachmentsPattern("**/*.txt"), hasKind(Kind.OK));
        assertThat(desc.doCheckAttachmentsPattern("logs/**/*.log"), hasKind(Kind.OK));

        // Directory traversal – error (changed from warning)
        assertThat(desc.doCheckAttachmentsPattern("../outside.txt"), hasKind(Kind.ERROR));
        assertThat(desc.doCheckAttachmentsPattern("a/b/../../c"), hasKind(Kind.ERROR));

        // Absolute path – error
        assertThat(desc.doCheckAttachmentsPattern("/tmp/attachment"), hasKind(Kind.ERROR));
        assertThat(desc.doCheckAttachmentsPattern("C:\\temp\\file"), hasKind(Kind.ERROR));

        // Unmatched braces – error
        assertThat(desc.doCheckAttachmentsPattern("file{"), hasKind(Kind.ERROR));
        assertThat(desc.doCheckAttachmentsPattern("file}"), hasKind(Kind.ERROR));

        // Balanced braces – OK
        assertThat(desc.doCheckAttachmentsPattern("file{abc}"), hasKind(Kind.OK));
    }

    @Test
    void testInlineAttachmentsPatternValidation() {
        ExtendedEmailPublisherDescriptor desc = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

        assertThat(desc.doCheckInlineAttachmentsPattern("**/*.png"), hasKind(Kind.OK));
        assertThat(desc.doCheckInlineAttachmentsPattern("../danger.png"), hasKind(Kind.ERROR));
    }
}
