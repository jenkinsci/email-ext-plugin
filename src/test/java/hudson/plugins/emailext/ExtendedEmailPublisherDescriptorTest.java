package hudson.plugins.emailext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.*;

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
import hudson.util.VersionNumber;
import jakarta.mail.Authenticator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import jenkins.model.Jenkins;
import org.htmlunit.ElementNotFoundException;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlDivision;
import org.htmlunit.html.HtmlNumberInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.htmlunit.html.HtmlTextArea;
import org.htmlunit.html.HtmlTextInput;
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

    private static void assertTitlePage(HtmlPage page) {
        String actualTitle = page.getTitleText();

        assertTrue(actualTitle.startsWith("System"), "Should be at the Configure System page");
    }

    @Test
    void testGlobalConfigDefaultState() throws Exception {
        HtmlPage page = j.createWebClient().goTo("configure");

        assertTitlePage(page);

        HtmlTextInput smtpHost = page.getElementByName("_.smtpHost");
        assertNotNull(smtpHost, "SMTP Server should be present");
        assertEquals("", smtpHost.getText(), "SMTP Server should be blank by default");

        HtmlNumberInput smtpPort = page.getElementByName("_.smtpPort");
        assertNotNull(smtpPort, "SMTP Port should be present");
        assertEquals("25", smtpPort.getText(), "SMTP Port should be 25 by default");

        HtmlTextInput defaultSuffix = page.getElementByName("_.defaultSuffix");
        assertNotNull(defaultSuffix, "Default suffix should be present");
        assertEquals("", defaultSuffix.getText(), "Default suffix should be blank by default");

        // default content type select control
        HtmlSelect contentType = page.getElementByName("_.defaultContentType");
        assertNotNull(contentType, "Content type selection should be present");
        assertEquals(
                "text/plain",
                contentType.getSelectedOptions().get(0).getValueAttribute(),
                "Plain text should be selected by default");

        HtmlCheckBoxInput precedenceBulk = page.getElementByName("_.precedenceBulk");
        assertNotNull(precedenceBulk, "Precedence Bulk should be present");
        assertFalse(precedenceBulk.isChecked(), "Add precedence bulk should not be checked by default");

        HtmlTextInput defaultRecipients = page.getElementByName("_.defaultRecipients");
        assertNotNull(defaultRecipients, "Default Recipients should be present");
        assertEquals("", defaultRecipients.getText(), "Default recipients should be blank by default");

        HtmlTextInput defaultReplyTo = page.getElementByName("_.defaultReplyTo");
        assertNotNull(defaultReplyTo, "Default Reply-to should be present");
        assertEquals("", defaultReplyTo.getText(), "Default Reply-To should be blank by default");

        HtmlTextInput emergencyReroute = page.getElementByName("_.emergencyReroute");
        assertNotNull(emergencyReroute, "Emergency Reroute should be present");
        assertEquals("", emergencyReroute.getText(), "Emergency Reroute should be blank by default");

        HtmlTextInput allowedDomains = page.getElementByName("_.allowedDomains");
        assertNotNull(allowedDomains, "Allowed Domains should be present");
        assertEquals("", allowedDomains.getText(), "Allowed Domains should be blank by default");

        HtmlTextInput excludedRecipients = page.getElementByName("_.excludedCommitters");
        assertNotNull(excludedRecipients, "Excluded Recipients should be present");
        assertEquals("", excludedRecipients.getText(), "Excluded Recipients should be blank by default");

        HtmlTextInput defaultSubject = page.getElementByName("_.defaultSubject");
        assertNotNull(defaultSubject, "Default Subject should be present");
        assertEquals(
                "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!",
                defaultSubject.getText(),
                "Default Subject should be set");

        HtmlNumberInput maxAttachmentSize = page.getElementByName("_.maxAttachmentSizeMb");
        assertNotNull(maxAttachmentSize, "Max attachment size should be present");
        assertThat(
                "Max attachment size should be blank or -1 by default",
                maxAttachmentSize.getText(),
                is(oneOf("", "-1")));

        HtmlTextArea defaultContent = page.getElementByName("_.defaultBody");
        assertNotNull(defaultContent, "Default content should be present");
        assertEquals(
                "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:\n\nCheck console output at $BUILD_URL to view the results.",
                defaultContent.getText(),
                "Default content should be set by default");

        HtmlTextArea defaultPresendScript = page.getElementByName("_.defaultPresendScript");
        assertNotNull(defaultPresendScript, "Default presend script should be present");
        assertEquals("", defaultPresendScript.getText(), "Default presend script should be blank by default");

        HtmlTextArea defaultPostsendScript = page.getElementByName("_.defaultPostsendScript");
        assertNotNull(defaultPostsendScript, "Default postsend script should be present");
        assertEquals("", defaultPostsendScript.getText(), "Default postsend script should be blank by default");

        HtmlCheckBoxInput debugMode = page.getElementByName("_.debugMode");
        assertNotNull(debugMode, "Debug mode should be present");
        assertFalse(debugMode.isChecked(), "Debug mode should not be checked by default");

        HtmlCheckBoxInput adminRequiredForTemplateTesting = page.getElementByName("_.adminRequiredForTemplateTesting");
        assertNotNull(adminRequiredForTemplateTesting, "Admin required for template testing should be present");
        assertFalse(
                adminRequiredForTemplateTesting.isChecked(),
                "Admin required for template testing should be unchecked by default");

        HtmlCheckBoxInput watchingEnabled = page.getElementByName("_.watchingEnabled");
        assertNotNull(watchingEnabled, "Watching enable should be present");
        assertFalse(watchingEnabled.isChecked(), "Watching enable should be unchecked by default");

        HtmlCheckBoxInput allowUnregisteredEnabled = page.getElementByName("_.allowUnregisteredEnabled");
        assertNotNull(allowUnregisteredEnabled, "Allow unregistered should be present");
        assertFalse(allowUnregisteredEnabled.isChecked(), "Allow unregistered should be unchecked by default");

        assertThrows(
                ElementNotFoundException.class,
                () -> page.getElementByName("defaultClasspath"),
                "defaultClasspath section should not be present");
    }

    @Test
    @Issue("JENKINS-20030")
    void testGlobalConfigSimpleRoundTrip() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlTextInput defaultRecipients = page.getElementByName("_.defaultRecipients");
        defaultRecipients.setValue("mickey@disney.com");
        j.submit(page.getFormByName("config"));

        assertEquals("mickey@disney.com", descriptor.getDefaultRecipients());
    }

    @Test
    @Issue("JENKINS-63367")
    void testSmtpPortRetainsSetValue() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        JenkinsRule.WebClient client = j.createWebClient();
        HtmlPage page = client.goTo("configure");
        HtmlNumberInput smtpPort = page.getElementByName("_.smtpPort");
        smtpPort.setValue("587");
        j.submit(page.getFormByName("config"));

        assertEquals("587", descriptor.getMailAccount().getSmtpPort());

        page = client.goTo("configure");
        smtpPort = page.getElementByName("_.smtpPort");
        assertEquals("587", smtpPort.getValue());
    }

    @Test
    @Issue("JENKINS-20133")
    void testPrecedenceBulkSettingRoundTrip() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlCheckBoxInput addPrecedenceBulk = page.getElementByName("_.precedenceBulk");
        addPrecedenceBulk.setChecked(true);
        j.submit(page.getFormByName("config"));

        assertTrue(descriptor.getPrecedenceBulk());
    }

    @Test
    @Issue("JENKINS-20133")
    void testListIDRoundTrip() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlTextInput listId = page.getElementByName("_.listId");
        listId.setValue("hammer");

        j.submit(page.getFormByName("config"));

        assertEquals("hammer", descriptor.getListId());
    }

    @Test
    void testAdvancedProperties() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlTextArea advProperties = page.getElementByName("_.advProperties");
        advProperties.setText("mail.smtp.starttls.enable=true");
        j.submit(page.getFormByName("config"));

        assertEquals(
                "mail.smtp.starttls.enable=true", descriptor.getMailAccount().getAdvProperties());
    }

    @Test
    void defaultTriggers() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");

        assertTitlePage(page);

        List<DomNode> nodes = page.getByXPath(".//button[contains(text(),'Default Triggers')]");
        assertEquals(1, nodes.size());
        HtmlButton defaultTriggers = (HtmlButton) nodes.get(0);
        defaultTriggers.click();

        String[] selectedTriggers = {
            "hudson.plugins.emailext.plugins.trigger.AbortedTrigger",
            "hudson.plugins.emailext.plugins.trigger.PreBuildTrigger",
            "hudson.plugins.emailext.plugins.trigger.FixedTrigger",
            "hudson.plugins.emailext.plugins.trigger.RegressionTrigger",
        };

        List<DomNode> failureTrigger =
                page.getByXPath(".//input[@json='hudson.plugins.emailext.plugins.trigger.FailureTrigger']");
        assertEquals(1, failureTrigger.size());
        HtmlCheckBoxInput failureTriggerCheckBox = (HtmlCheckBoxInput) failureTrigger.get(0);
        assertTrue(failureTriggerCheckBox.isChecked());
        failureTriggerCheckBox.setChecked(false);

        for (String selectedTrigger : selectedTriggers) {
            List<DomNode> triggerItems =
                    page.getByXPath(".//input[@name='_.defaultTriggerIds' and @json='" + selectedTrigger + "']");
            assertEquals(1, triggerItems.size());
            HtmlCheckBoxInput checkBox = (HtmlCheckBoxInput) triggerItems.get(0);
            checkBox.setChecked(true);
        }

        j.submit(page.getFormByName("config"));
        assertArrayEquals(selectedTriggers, descriptor.getDefaultTriggerIds().toArray(new String[0]));
    }

    @Test
    void groovyClassPath() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");

        assertTitlePage(page);

        List<DomElement> nodes = page.getByXPath(
                ".//div[contains(@class, 'setting-name') and ./text()='Additional groovy classpath'] | .//div[contains(@class, 'jenkins-form-label') and ./text()='Additional groovy classpath']");
        assertEquals(1, nodes.size());
        HtmlDivision settingName = (HtmlDivision) nodes.get(0);

        nodes = settingName.getByXPath(
                "../div[contains(@class, 'setting-name')]/div[@class='repeated-container']/div[@name='defaultClasspath']");
        assertEquals(0, nodes.size(), "Should not have any class path setup by default");

        HtmlDivision div;
        HtmlButton addButton;
        if (Jenkins.getVersion().isOlderThan(new VersionNumber("2.409"))) {
            nodes = settingName.getByXPath(
                    "../div[@class='setting-main']/div[@class='repeated-container' and span[starts-with(@id, 'yui-gen')]/span[@class='first-child']/button[./text()='Add']]");
            assertEquals(1, nodes.size());
            div = (HtmlDivision) nodes.get(0);
            nodes = div.getByXPath(".//button[./text()='Add']");
            addButton = (HtmlButton) nodes.get(0);
        } else {
            nodes = settingName.getByXPath(
                    "../div[@class='setting-main']/div[@class='repeated-container']/button[./text()='Add']");
            assertEquals(1, nodes.size());
            addButton = (HtmlButton) nodes.get(0);
        }
        addButton.click();

        nodes = settingName.getByXPath(
                "../div[@class='setting-main']/div[@class='repeated-container']/div[@name='defaultClasspath']");
        assertEquals(1, nodes.size());
        div = (HtmlDivision) nodes.get(0);
        String divClass = div.getAttribute("class");
        assertTrue(divClass.contains("first") && divClass.contains("last") && divClass.contains("only"));

        nodes = div.getByXPath(".//input[@name='_.path' and @type='text']");
        assertEquals(1, nodes.size());

        HtmlTextInput path = (HtmlTextInput) nodes.get(0);
        path.setText("/path/to/classes");

        addButton.click();

        nodes = settingName.getByXPath(
                "../div[@class='setting-main']/div[@class='repeated-container']/div[@name='defaultClasspath' and contains(@class, 'last')]");
        assertEquals(1, nodes.size());
        div = (HtmlDivision) nodes.get(0);
        divClass = div.getAttribute("class");
        assertTrue(divClass.contains("last"));
        assertFalse(divClass.contains("first") || divClass.contains("only"));

        nodes = div.getByXPath(".//input[@name='_.path' and @type='text']");
        assertEquals(1, nodes.size());
        path = (HtmlTextInput) nodes.get(0);
        path.setText("/other/path/to/classes");

        j.submit(page.getFormByName("config"));

        String[] classpath = {
            "/path/to/classes", "/other/path/to/classes",
        };

        assertArrayEquals(
                classpath,
                descriptor.getDefaultClasspath().stream()
                        .map(GroovyScriptPath::getPath)
                        .toArray(String[]::new));
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
            Collection<Descriptor> descriptors = Functions.getSortedDescriptorsForGlobalConfigUnclassified();
            Optional<Descriptor> found = descriptors.stream()
                    .filter(descriptor -> descriptor instanceof ExtendedEmailPublisherDescriptor)
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

    @Test
    void testFixEmptyAndTrimNormal() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        // add a credential to the GLOBAL scope
        UsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "email-ext", "Username/password for SMTP", "smtpUsername", "password");
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), c);

        MailAccount ma = new MailAccount();
        ma.setAddress("example@example.com");
        ma.setSmtpHost("smtp.example.com");
        ma.setSmtpPort("25");
        ma.setCredentialsId("email-ext");

        descriptor.setMailAccount(ma);
        descriptor.setDefaultSuffix("@example.com");
        descriptor.setCharset("UTF-8");
        descriptor.setEmergencyReroute("emergency@example.com");
        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        assertEquals("example@example.com", descriptor.getMailAccount().getAddress());
        assertEquals("smtp.example.com", descriptor.getMailAccount().getSmtpHost());
        assertEquals("25", descriptor.getMailAccount().getSmtpPort());
        assertEquals("email-ext", descriptor.getMailAccount().getCredentialsId());
        assertEquals("@example.com", descriptor.getDefaultSuffix());
        assertEquals("UTF-8", descriptor.getCharset());
        assertEquals("emergency@example.com", descriptor.getEmergencyReroute());
    }

    @Test
    void testFixEmptyAndTrimExtraSpaces() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        // add a credential to the GLOBAL scope
        UsernamePasswordCredentials c = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "email-ext", "Username/password for SMTP", "smtpUsername", "password");
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), c);

        MailAccount ma = new MailAccount();
        ma.setAddress("       example@example.com      ");
        ma.setSmtpHost("      smtp.example.com      ");
        ma.setSmtpPort("      25      ");
        ma.setCredentialsId("     email-ext     ");

        descriptor.setMailAccount(ma);
        descriptor.setDefaultSuffix("      @example.com      ");
        descriptor.setCharset("      UTF-8      ");
        descriptor.setEmergencyReroute("      emergency@example.com      ");
        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        assertEquals("example@example.com", descriptor.getMailAccount().getAddress());
        assertEquals("smtp.example.com", descriptor.getMailAccount().getSmtpHost());
        assertEquals("25", descriptor.getMailAccount().getSmtpPort());
        assertEquals("email-ext", descriptor.getMailAccount().getCredentialsId());
        assertEquals("@example.com", descriptor.getDefaultSuffix());
        assertEquals("UTF-8", descriptor.getCharset());
        assertEquals("emergency@example.com", descriptor.getEmergencyReroute());
    }

    @Test
    void testFixEmptyAndTrimEmptyString() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        MailAccount ma = new MailAccount();
        ma.setAddress("");
        ma.setSmtpHost("");
        ma.setSmtpPort("");
        ma.setCredentialsId("");

        descriptor.setMailAccount(ma);
        descriptor.setDefaultSuffix("");
        descriptor.setCharset("");
        descriptor.setEmergencyReroute("");
        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        assertNull(descriptor.getMailAccount().getAddress());
        assertNull(descriptor.getMailAccount().getSmtpHost());
        assertEquals("25", descriptor.getMailAccount().getSmtpPort());
        assertNull(descriptor.getMailAccount().getCredentialsId());
        assertNull(descriptor.getDefaultSuffix());
        assertEquals("UTF-8", descriptor.getCharset());
        assertEquals("", descriptor.getEmergencyReroute());
    }

    @Test
    void testFixEmptyAndTrimNull() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        MailAccount ma = new MailAccount();
        ma.setAddress(null);
        ma.setSmtpHost(null);
        ma.setSmtpPort(null);
        ma.setCredentialsId(null);

        descriptor.setMailAccount(ma);
        descriptor.setDefaultSuffix(null);
        descriptor.setCharset(null);
        descriptor.setEmergencyReroute(null);
        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        assertNull(descriptor.getMailAccount().getAddress());
        assertNull(descriptor.getMailAccount().getSmtpHost());
        assertEquals("25", descriptor.getMailAccount().getSmtpPort());
        assertNull(descriptor.getMailAccount().getCredentialsId());
        assertNull(descriptor.getDefaultSuffix());
        assertEquals("UTF-8", descriptor.getCharset());
        assertEquals("", descriptor.getEmergencyReroute());
    }

    @LocalData
    @Test
    void persistedConfigurationBeforeJCasC() {
        // Local data created using Email Extension 2.71 with the following code:
        /*
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlForm config = page.getFormByName("config");
        j.submit(config);

        page = j.createWebClient().goTo("configure");
        config = page.getFormByName("config");
        j.getButtonByCaption(config, "Advanced...").click();
        j.getButtonByCaption(config, "Default Triggers...").click();
        WebClientUtil.waitForJSExec(page.getWebClient());
        HtmlCheckBoxInput useSmtpAuth = page.getElementByName("ext_mailer_use_smtp_auth");
        useSmtpAuth.click();
        HtmlCheckBoxInput useListId = page.getElementByName("ext_mailer_use_list_id");
        useListId.click();
        WebClientUtil.waitForJSExec(page.getWebClient());
        Set<HtmlButton> buttons = new HashSet<>();
        for (HtmlElement button : config.getElementsByTagName("button")) {
            buttons.add((HtmlButton) button);
        }
        for (HtmlButton button : buttons) {
            DomNode ancestor =
                    button.getParentNode().getParentNode().getParentNode().getParentNode();
            String key = ancestor.getPreviousSibling().getTextContent().trim();
            if (key.equals("Additional accounts") || key.equals("Additional groovy classpath")) {
                button.click();
            }
        }
        WebClientUtil.waitForJSExec(page.getWebClient());
        HtmlCheckBoxInput useSmtpAuth2 = page.getElementByName("_.auth");
        useSmtpAuth2.click();
        WebClientUtil.waitForJSExec(page.getWebClient());

        HtmlTextInput address = page.getElementByName("_.adminAddress");
        address.setValue("admin@example.com");
        HtmlTextInput smtpServer = page.getElementByName("ext_mailer_smtp_server");
        smtpServer.setValue("smtp.example.com");
        HtmlTextInput defaultSuffix = page.getElementByName("ext_mailer_default_suffix");
        defaultSuffix.setValue("@example.com");
        HtmlTextInput smtpUsername = page.getElementByName("ext_mailer_smtp_username");
        smtpUsername.setValue("admin");
        HtmlPasswordInput smtpPassword = page.getElementByName("ext_mailer_smtp_password");
        smtpPassword.setValue("honeycomb");
        HtmlTextArea advProperties = page.getElementByName("ext_mailer_adv_properties");
        advProperties.setText("mail.smtp.ssl.trust=example.com");
        HtmlCheckBoxInput smtpUseSsl = page.getElementByName("ext_mailer_smtp_use_ssl");
        smtpUseSsl.click();
        HtmlTextInput smtpPort = page.getElementByName("ext_mailer_smtp_port");
        smtpPort.setValue("2525");
        HtmlTextInput charset = page.getElementByName("ext_mailer_charset");
        charset.setValue("UTF-8");
        HtmlTextInput address2 = page.getElementByName("_.address");
        address2.setValue("admin@example2.com");
        HtmlTextInput smtpServer2 = page.getElementByName("_.smtpHost");
        smtpServer2.setValue("smtp.example2.com");
        HtmlTextInput smtpPort2 = page.getElementByName("_.smtpPort");
        smtpPort2.setValue("2626");
        HtmlTextInput smtpUsername2 = page.getElementByName("_.smtpUsername");
        smtpUsername2.setValue("admin2");
        HtmlPasswordInput smtpPassword2 = page.getElementByName("_.smtpPassword");
        smtpPassword2.setValue("honeycomb2");
        HtmlCheckBoxInput smtpUseSsl2 = page.getElementByName("_.useSsl");
        smtpUseSsl2.click();
        HtmlTextArea advProperties2 = page.getElementByName("_.advProperties");
        advProperties2.setText("mail.smtp.ssl.trust=example2.com");
        HtmlSelect defaultContentType = page.getElementByName("ext_mailer_default_content_type");
        defaultContentType.setSelectedAttribute("text/html", true);
        HtmlTextInput listId = page.getElementByName("ext_mailer_list_id");
        listId.setValue("<list.example.com>");
        HtmlCheckBoxInput addPrecedenceBulk =
                page.getElementByName("ext_mailer_add_precedence_bulk");
        addPrecedenceBulk.click();
        HtmlTextInput defaultRecipients = page.getElementByName("ext_mailer_default_recipients");
        defaultRecipients.setValue("default@example.com");
        HtmlTextInput defaultReplyto = page.getElementByName("ext_mailer_default_replyto");
        defaultReplyto.setValue("noreply@example.com");
        HtmlTextInput emergencyReroute = page.getElementByName("ext_mailer_emergency_reroute");
        emergencyReroute.setValue("emergency@example.com");
        HtmlTextInput allowedDomains = page.getElementByName("ext_mailer_allowed_domains");
        allowedDomains.setValue("@example.com");
        HtmlTextInput excludedCommitters = page.getElementByName("ext_mailer_excluded_committers");
        excludedCommitters.setValue("excluded@example.com");
        HtmlTextInput defaultSubject = page.getElementByName("ext_mailer_default_subject");
        defaultSubject.setValue("$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS");
        HtmlTextInput maxAttachmentSize = page.getElementByName("ext_mailer_max_attachment_size");
        maxAttachmentSize.setValue("42");
        HtmlTextArea defaultBody = page.getElementByName("ext_mailer_default_body");
        defaultBody.setText("$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS");
        HtmlTextArea defaultPresendScript =
                page.getElementByName("ext_mailer_default_presend_script");
        defaultPresendScript.setText("build.previousBuild.result.toString().equals('FAILURE')");
        HtmlTextArea defaultPostsendScript =
                page.getElementByName("ext_mailer_default_postsend_script");
        defaultPostsendScript.setText("build.result.toString().equals('FAILURE')");
        HtmlTextInput defaultClasspath = page.getElementByName("ext_mailer_default_classpath");
        defaultClasspath.setValue("classes");
        HtmlCheckBoxInput debugMode = page.getElementByName("ext_mailer_debug_mode");
        debugMode.click();
        HtmlCheckBoxInput requireAdminForTemplateTesting =
                page.getElementByName("ext_mailer_require_admin_for_template_testing");
        requireAdminForTemplateTesting.click();
        HtmlCheckBoxInput watchingEnabled = page.getElementByName("ext_mailer_watching_enabled");
        watchingEnabled.click();
        HtmlCheckBoxInput allowUnregisteredEnabled =
                page.getElementByName("ext_mailer_allow_unregistered_enabled");
        allowUnregisteredEnabled.click();
        for (HtmlInput input : config.getInputsByName("defaultTriggers")) {
            input.click();
        }

        WebClientUtil.waitForJSExec(page.getWebClient());
        j.submit(config);
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
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlForm config = page.getFormByName("config");
        j.submit(config);

        page = j.createWebClient().goTo("configure");
        config = page.getFormByName("config");
        for (HtmlElement button : config.getElementsByTagName("button")) {
            if (button.getTextContent().trim().equals("Advanced...")) {
                button.click();
            }
        }
        j.getButtonByCaption(config, "Default Triggers...").click();
        WebClientUtil.waitForJSExec(page.getWebClient());
        Set<HtmlButton> buttons = new HashSet<>();
        for (HtmlElement button : config.getElementsByTagName("button")) {
            buttons.add((HtmlButton) button);
        }
        for (HtmlButton button : buttons) {
            DomNode ancestor =
                    button.getParentNode().getParentNode().getParentNode().getParentNode();
            String key = ancestor.getPreviousSibling().getTextContent().trim();
            if (key.equals("Additional accounts") || key.equals("Additional groovy classpath")) {
                button.click();
            }
        }
        WebClientUtil.waitForJSExec(page.getWebClient());
        buttons = new HashSet<>();
        for (HtmlElement button : config.getElementsByTagName("button")) {
            buttons.add((HtmlButton) button);
        }
        for (HtmlButton button : buttons) {
            String textContent = button.getTextContent().trim();
            if (textContent.equals("Advanced...")) {
                DomNode ancestor =
                        button.getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode();
                String key = ancestor.getPreviousSibling().getTextContent().trim();
                if (key.equals("Additional accounts")) {
                    button.click();
                }
            }
        }
        WebClientUtil.waitForJSExec(page.getWebClient());

        HtmlTextInput address = page.getElementByName("_.adminAddress");
        address.setValue("admin@example.com");
        List<DomElement> smtpServers = page.getElementsByName("_.smtpHost");
        HtmlTextInput smtpServer = (HtmlTextInput) smtpServers.get(0);
        smtpServer.setValue("smtp.example.com");
        List<DomElement> smtpPorts = page.getElementsByName("_.smtpPort");
        HtmlNumberInput smtpPort = (HtmlNumberInput) smtpPorts.get(0);
        smtpPort.setValue("2525");
        List<DomElement> smtpUsernames = page.getElementsByName("_.smtpUsername");
        HtmlTextInput smtpUsername = (HtmlTextInput) smtpUsernames.get(0);
        smtpUsername.setValue("admin");
        List<DomElement> smtpPasswords = page.getElementsByName("_.smtpPassword");
        HtmlTextInput smtpPassword = (HtmlTextInput) smtpPasswords.get(0);
        smtpPassword.setValue("honeycomb");
        List<DomElement> smtpUseSsls = page.getElementsByName("_.useSsl");
        HtmlCheckBoxInput smtpUseSsl = (HtmlCheckBoxInput) smtpUseSsls.get(0);
        smtpUseSsl.click();
        List<DomElement> advPropertiesElements = page.getElementsByName("_.advProperties");
        HtmlTextArea advProperties = (HtmlTextArea) advPropertiesElements.get(0);
        advProperties.setText("mail.smtp.ssl.trust=example.com");
        HtmlTextInput defaultSuffix = page.getElementByName("_.defaultSuffix");
        defaultSuffix.setValue("@example.com");
        HtmlTextInput charset = page.getElementByName("_.charset");
        charset.setValue("UTF-8");
        List<DomElement> addresses = page.getElementsByName("_.address");
        HtmlTextInput address2 = (HtmlTextInput) addresses.get(1);
        address2.setValue("admin@example2.com");
        HtmlTextInput smtpServer2 = (HtmlTextInput) smtpServers.get(1);
        smtpServer2.setValue("smtp.example2.com");
        HtmlNumberInput smtpPort2 = (HtmlNumberInput) smtpPorts.get(1);
        smtpPort2.setValue("2626");
        HtmlTextInput smtpUsername2 = (HtmlTextInput) smtpUsernames.get(1);
        smtpUsername2.setValue("admin2");
        HtmlTextInput smtpPassword2 = (HtmlTextInput) smtpPasswords.get(1);
        smtpPassword2.setValue("honeycomb2");
        HtmlCheckBoxInput smtpUseSsl2 = (HtmlCheckBoxInput) smtpUseSsls.get(1);
        smtpUseSsl2.click();
        HtmlTextArea advProperties2 = (HtmlTextArea) advPropertiesElements.get(1);
        advProperties2.setText("mail.smtp.ssl.trust=example2.com");
        HtmlSelect defaultContentType = page.getElementByName("_.defaultContentType");
        defaultContentType.setSelectedAttribute("text/html", true);
        HtmlTextInput listId = page.getElementByName("_.listId");
        listId.setValue("<list.example.com>");
        HtmlCheckBoxInput addPrecedenceBulk = page.getElementByName("_.precedenceBulk");
        addPrecedenceBulk.click();
        HtmlTextInput defaultRecipients = page.getElementByName("_.defaultRecipients");
        defaultRecipients.setValue("default@example.com");
        HtmlTextInput defaultReplyto = page.getElementByName("_.defaultReplyTo");
        defaultReplyto.setValue("noreply@example.com");
        HtmlTextInput emergencyReroute = page.getElementByName("_.emergencyReroute");
        emergencyReroute.setValue("emergency@example.com");
        HtmlTextInput allowedDomains = page.getElementByName("_.allowedDomains");
        allowedDomains.setValue("@example.com");
        HtmlTextInput excludedCommitters = page.getElementByName("_.excludedCommitters");
        excludedCommitters.setValue("excluded@example.com");
        HtmlTextInput defaultSubject = page.getElementByName("_.defaultSubject");
        defaultSubject.setValue("$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS");
        HtmlNumberInput maxAttachmentSize = page.getElementByName("_.maxAttachmentSizeMb");
        maxAttachmentSize.setValue("42");
        HtmlTextArea defaultBody = page.getElementByName("_.defaultBody");
        defaultBody.setText("$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS");
        HtmlTextArea defaultPresendScript = page.getElementByName("_.defaultPresendScript");
        defaultPresendScript.setText("build.previousBuild.result.toString().equals('FAILURE')");
        HtmlTextArea defaultPostsendScript = page.getElementByName("_.defaultPostsendScript");
        defaultPostsendScript.setText("build.result.toString().equals('FAILURE')");
        HtmlTextInput defaultClasspath = page.getElementByName("_.path");
        defaultClasspath.setValue("classes");
        HtmlCheckBoxInput debugMode = page.getElementByName("_.debugMode");
        debugMode.click();
        HtmlCheckBoxInput requireAdminForTemplateTesting =
                page.getElementByName("_.adminRequiredForTemplateTesting");
        requireAdminForTemplateTesting.click();
        HtmlCheckBoxInput watchingEnabled = page.getElementByName("_.watchingEnabled");
        watchingEnabled.click();
        HtmlCheckBoxInput allowUnregisteredEnabled =
                page.getElementByName("_.allowUnregisteredEnabled");
        allowUnregisteredEnabled.click();
        for (HtmlInput input : config.getInputsByName("_.defaultTriggerIds")) {
            input.click();
        }

        WebClientUtil.waitForJSExec(page.getWebClient());
        j.submit(config);
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
        // add two credentials to the GLOBAL scope
        UsernamePasswordCredentials c1 = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "email-ext-admin", "Username/password for SMTP", "admin", "honeycomb");
        UsernamePasswordCredentials c2 = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "email-ext-admin2", "Username/password for SMTP", "admin2", "honeycomb2");
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), c1);
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), c2);

        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlForm config = page.getFormByName("config");
        j.submit(config);

        page = j.createWebClient().goTo("configure");
        config = page.getFormByName("config");
        for (HtmlElement button : config.getElementsByTagName("button")) {
            if (button.getTextContent().trim().equals("Advanced...")) {
                button.click();
            }
        }
        j.getButtonByCaption(config, "Default Triggers...").click();
        WebClientUtil.waitForJSExec(page.getWebClient());
        Set<HtmlButton> buttons = new HashSet<>();
        for (HtmlElement button : config.getElementsByTagName("button")) {
            buttons.add((HtmlButton) button);
        }
        for (HtmlButton button : buttons) {
            DomNode ancestor =
                    button.getParentNode().getParentNode().getParentNode().getParentNode();
            if(ancestor.getPreviousSibling() == null) {
                continue;
            }
            String key = ancestor.getPreviousSibling().getTextContent().trim();
            if (key.equals("Additional accounts") || key.equals("Additional groovy classpath")) {
                button.click();
            }
        }
        WebClientUtil.waitForJSExec(page.getWebClient());
        buttons = new HashSet<>();
        for (HtmlElement button : config.getElementsByTagName("button")) {
            buttons.add((HtmlButton) button);
        }
        for (HtmlButton button : buttons) {
            String textContent = button.getTextContent().trim();
            if (textContent.equals("Advanced...")) {
                DomNode ancestor =
                        button.getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode()
                                .getParentNode();
                String key = ancestor.getPreviousSibling().getTextContent().trim();
                if (key.equals("Additional accounts")) {
                    button.click();
                }
            }
        }
        WebClientUtil.waitForJSExec(page.getWebClient());

        HtmlTextInput address = page.getElementByName("_.adminAddress");
        address.setValue("admin@example.com");
        List<DomElement> smtpServers = page.getElementsByName("_.smtpHost");
        HtmlTextInput smtpServer = (HtmlTextInput) smtpServers.get(0);
        smtpServer.setValue("smtp.example.com");
        List<DomElement> smtpPorts = page.getElementsByName("_.smtpPort");
        HtmlNumberInput smtpPort = (HtmlNumberInput) smtpPorts.get(0);
        smtpPort.setValue("2525");
        List<DomElement> credentialsIds = page.getElementsByName("_.credentialsId");
        HtmlSelect credentialsId = (HtmlSelect)credentialsIds.get(0);
        List<HtmlOption> options = credentialsId.getOptions();
        credentialsId.setSelectedIndex(1);
        List<DomElement> smtpUseSsls = page.getElementsByName("_.useSsl");
        HtmlCheckBoxInput smtpUseSsl = (HtmlCheckBoxInput) smtpUseSsls.get(0);
        smtpUseSsl.click();
        List<DomElement> advPropertiesElements = page.getElementsByName("_.advProperties");
        HtmlTextArea advProperties = (HtmlTextArea) advPropertiesElements.get(0);
        advProperties.setText("mail.smtp.ssl.trust=example.com");
        HtmlTextInput defaultSuffix = page.getElementByName("_.defaultSuffix");
        defaultSuffix.setValue("@example.com");
        HtmlTextInput charset = page.getElementByName("_.charset");
        charset.setValue("UTF-8");
        List<DomElement> addresses = page.getElementsByName("_.address");
        HtmlTextInput address2 = (HtmlTextInput) addresses.get(1);
        address2.setValue("admin@example2.com");
        HtmlTextInput smtpServer2 = (HtmlTextInput) smtpServers.get(1);
        smtpServer2.setValue("smtp.example2.com");
        HtmlNumberInput smtpPort2 = (HtmlNumberInput) smtpPorts.get(1);
        smtpPort2.setValue("2626");
        HtmlSelect credentialId2 = (HtmlSelect)credentialsIds.get(1);
        credentialId2.setSelectedIndex(2);
        HtmlCheckBoxInput smtpUseSsl2 = (HtmlCheckBoxInput) smtpUseSsls.get(1);
        smtpUseSsl2.click();
        HtmlTextArea advProperties2 = (HtmlTextArea) advPropertiesElements.get(1);
        advProperties2.setText("mail.smtp.ssl.trust=example2.com");
        HtmlSelect defaultContentType = page.getElementByName("_.defaultContentType");
        defaultContentType.setSelectedAttribute("text/html", true);
        HtmlTextInput listId = page.getElementByName("_.listId");
        listId.setValue("<list.example.com>");
        HtmlCheckBoxInput addPrecedenceBulk = page.getElementByName("_.precedenceBulk");
        addPrecedenceBulk.click();
        HtmlTextInput defaultRecipients = page.getElementByName("_.defaultRecipients");
        defaultRecipients.setValue("default@example.com");
        HtmlTextInput defaultReplyto = page.getElementByName("_.defaultReplyTo");
        defaultReplyto.setValue("noreply@example.com");
        HtmlTextInput emergencyReroute = page.getElementByName("_.emergencyReroute");
        emergencyReroute.setValue("emergency@example.com");
        HtmlTextInput allowedDomains = page.getElementByName("_.allowedDomains");
        allowedDomains.setValue("@example.com");
        HtmlTextInput excludedCommitters = page.getElementByName("_.excludedCommitters");
        excludedCommitters.setValue("excluded@example.com");
        HtmlTextInput defaultSubject = page.getElementByName("_.defaultSubject");
        defaultSubject.setValue("$PROJECT_NAME - Build #$BUILD_NUMBER - $BUILD_STATUS");
        HtmlNumberInput maxAttachmentSize = page.getElementByName("_.maxAttachmentSizeMb");
        maxAttachmentSize.setValue("42");
        HtmlTextArea defaultBody = page.getElementByName("_.defaultBody");
        defaultBody.setText("$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS");
        HtmlTextArea defaultPresendScript = page.getElementByName("_.defaultPresendScript");
        defaultPresendScript.setText("build.previousBuild.result.toString().equals('FAILURE')");
        HtmlTextArea defaultPostsendScript = page.getElementByName("_.defaultPostsendScript");
        defaultPostsendScript.setText("build.result.toString().equals('FAILURE')");
        HtmlTextInput defaultClasspath = page.getElementByName("_.path");
        defaultClasspath.setValue("classes");
        HtmlCheckBoxInput debugMode = page.getElementByName("_.debugMode");
        debugMode.click();
        HtmlCheckBoxInput requireAdminForTemplateTesting =
                page.getElementByName("_.adminRequiredForTemplateTesting");
        requireAdminForTemplateTesting.click();
        HtmlCheckBoxInput watchingEnabled = page.getElementByName("_.watchingEnabled");
        watchingEnabled.click();
        HtmlCheckBoxInput allowUnregisteredEnabled =
                page.getElementByName("_.allowUnregisteredEnabled");
        allowUnregisteredEnabled.click();
        for (HtmlInput input : config.getInputsByName("_.defaultTriggerIds")) {
            input.click();
        }

        WebClientUtil.waitForJSExec(page.getWebClient());
        j.submit(config);
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
}
