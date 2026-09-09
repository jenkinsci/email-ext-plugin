package hudson.plugins.emailext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.plugins.emailext.plugins.trigger.AbortedTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedTrigger;
import hudson.plugins.emailext.plugins.trigger.PreBuildTrigger;
import hudson.plugins.emailext.plugins.trigger.RegressionTrigger;
import java.util.List;
import jenkins.model.Jenkins;
import org.htmlunit.ElementNotFoundException;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlDivision;
import org.htmlunit.html.HtmlForm;
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

@WithJenkins
class ExtendedEmailGlobalConfigurationTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @Test
    void testAutoConfigureFallsBackToStandaloneConfigurationInstance() {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        assertNotNull(config);
        assertEquals(ExtendedEmailPublisher.DEFAULT_SUBJECT_TEXT, config.getDefaultSubject());
        assertEquals(ExtendedEmailPublisher.DEFAULT_BODY_TEXT, config.getDefaultBody());
    }

    /**
     * Creates a WebClient that does not throw on non-2xx responses.
     * Needed because doConfigure() redirects to /manage on success, which may
     * return a non-2xx status in the test environment.
     */
    private JenkinsRule.WebClient createWebClient() {
        JenkinsRule.WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);
        return wc;
    }

    @Test
    void testGlobalConfigDefaultState() throws Exception {
        try (JenkinsRule.WebClient client = createWebClient()) {
            HtmlPage page = client.goTo("manage/email-ext/");

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

            HtmlCheckBoxInput adminRequiredForTemplateTesting =
                    page.getElementByName("_.adminRequiredForTemplateTesting");
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
    }

    @Test
    @Issue("JENKINS-20030")
    void testGlobalConfigSimpleRoundTrip() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        try (JenkinsRule.WebClient client = createWebClient()) {
            HtmlPage page = client.goTo("manage/email-ext/");
            HtmlTextInput defaultRecipients = page.getElementByName("_.defaultRecipients");
            defaultRecipients.setValue("mickey@disney.com");
            j.submit(page.getFormByName("config"));

            assertEquals("mickey@disney.com", config.getDefaultRecipients());
        }
    }

    @Test
    @Issue("JENKINS-53898")
    void testDefaultAttachBuildLogRoundTrip() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        HtmlPage page;
        HtmlSelect defaultAttachBuildLog;
        try (JenkinsRule.WebClient client = createWebClient()) {
            page = client.goTo("manage/email-ext/");

            defaultAttachBuildLog = page.getElementByName("defaultAttachBuildLogMode");
            assertNotNull(defaultAttachBuildLog, "Default Attach Build Log select should be present in global config");
            assertEquals(
                    "NONE",
                    defaultAttachBuildLog.getSelectedOptions().get(0).getValueAttribute(),
                    "Do Not Attach Build Log should be selected by default");

            // Change to "Attach Build Log" (value=1)
            defaultAttachBuildLog.setSelectedAttribute(AttachBuildLogMode.ATTACH.toString(), true);
            j.submit(page.getFormByName("config"));
            assertEquals(
                    AttachBuildLogMode.ATTACH,
                    config.getDefaultAttachBuildLogMode(),
                    "Config should reflect the selected global default (Attach Build Log)");
        }

        // Change to "Compress and Attach Build Log" (value=2)
        try (JenkinsRule.WebClient client = createWebClient()) {
            page = client.goTo("manage/email-ext/");
            defaultAttachBuildLog = page.getElementByName("defaultAttachBuildLogMode");
            defaultAttachBuildLog.setSelectedAttribute(AttachBuildLogMode.COMPRESS_AND_ATTACH.toString(), true);
            j.submit(page.getFormByName("config"));
            assertEquals(
                    AttachBuildLogMode.COMPRESS_AND_ATTACH,
                    config.getDefaultAttachBuildLogMode(),
                    "Config should reflect the selected global default (Compress and Attach)");
        }
    }

    @Test
    @Issue("JENKINS-63367")
    void testSmtpPortRetainsSetValue() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        try (JenkinsRule.WebClient client = createWebClient()) {
            HtmlPage page = client.goTo("manage/email-ext/");
            HtmlNumberInput smtpPort = page.getElementByName("_.smtpPort");
            smtpPort.setValue("587");
            j.submit(page.getFormByName("config"));

            assertEquals("587", config.getMailAccount().getSmtpPort());

            page = client.goTo("email-ext");
            smtpPort = page.getElementByName("_.smtpPort");
            assertEquals("587", smtpPort.getValue());
        }
    }

    @Test
    @Issue("JENKINS-20133")
    void testPrecedenceBulkSettingRoundTrip() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        try (JenkinsRule.WebClient client = createWebClient()) {
            HtmlPage page = client.goTo("manage/email-ext/");
            HtmlCheckBoxInput addPrecedenceBulk = page.getElementByName("_.precedenceBulk");
            addPrecedenceBulk.setChecked(true);
            j.submit(page.getFormByName("config"));

            assertTrue(config.getPrecedenceBulk());
        }
    }

    @Test
    @Issue("JENKINS-20133")
    void testListIDRoundTrip() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        try (JenkinsRule.WebClient client = createWebClient()) {
            HtmlPage page = client.goTo("manage/email-ext/");
            HtmlTextInput listId = page.getElementByName("_.listId");
            listId.setValue("hammer");

            j.submit(page.getFormByName("config"));

            assertEquals("hammer", config.getListId());
        }
    }

    @Test
    void testAdvancedProperties() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        try (JenkinsRule.WebClient client = createWebClient()) {
            HtmlPage page = client.goTo("manage/email-ext/");
            HtmlTextArea advProperties = page.getElementByName("_.advProperties");
            advProperties.setText("mail.smtp.starttls.enable=true");
            j.submit(page.getFormByName("config"));

            assertEquals(
                    "mail.smtp.starttls.enable=true", config.getMailAccount().getAdvProperties());
        }
    }

    @Test
    void defaultTriggers() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        try (JenkinsRule.WebClient client = createWebClient()) {
            HtmlPage page = client.goTo("manage/email-ext/");

            List<DomNode> nodes = page.getByXPath(".//button[contains(text(),'Default Triggers')]");
            assertEquals(1, nodes.size());
            HtmlButton defaultTriggers = (HtmlButton) nodes.get(0);
            defaultTriggers.click();

            String[] selectedTriggers = {
                AbortedTrigger.class.getName(),
                PreBuildTrigger.class.getName(),
                FixedTrigger.class.getName(),
                RegressionTrigger.class.getName(),
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
            assertArrayEquals(selectedTriggers, config.getDefaultTriggerIds().toArray(new String[0]));
        }
    }

    @Test
    void groovyClassPath() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        try (JenkinsRule.WebClient client = createWebClient()) {
            HtmlPage page = client.goTo("manage/email-ext/");

            List<DomElement> nodes = page.getByXPath(
                    ".//div[contains(@class, 'setting-name') and ./text()='Additional groovy classpath'] | .//div[contains(@class, 'jenkins-form-label') and ./text()='Additional groovy classpath']");
            assertEquals(1, nodes.size());
            HtmlDivision settingName = (HtmlDivision) nodes.get(0);

            nodes = settingName.getByXPath(
                    "../div[contains(@class, 'setting-name')]/div[@class='repeated-container']/div[@name='defaultClasspath']");
            assertEquals(0, nodes.size(), "Should not have any class path setup by default");

            HtmlDivision div;
            HtmlButton addButton;
            nodes = settingName.getByXPath(
                    "../div[@class='setting-main']/div[@class='repeated-container']/button[normalize-space(string(.)) = 'Add']");
            assertEquals(1, nodes.size());
            addButton = (HtmlButton) nodes.get(0);
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
                    config.getDefaultClasspath().stream()
                            .map(GroovyScriptPath::getPath)
                            .toArray(String[]::new));
        }
    }

    @Test
    void testFixEmptyAndTrimNormal() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        // add a credential to the GLOBAL scope
        UsernamePasswordCredentialsImpl c = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "email-ext", "Username/password for SMTP", "smtpUsername", "password");
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), c);

        MailAccount ma = new MailAccount();
        ma.setAddress("example@example.com");
        ma.setSmtpHost("smtp.example.com");
        ma.setSmtpPort("25");
        ma.setCredentialsId("email-ext");

        config.setMailAccount(ma);
        config.setDefaultSuffix("@example.com");
        config.setCharset("UTF-8");
        config.setEmergencyReroute("emergency@example.com");
        try (JenkinsRule.WebClient client = createWebClient()) {
            j.submit(client.goTo("manage/email-ext/").getFormByName("config"));
        }
        assertEquals("example@example.com", config.getMailAccount().getAddress());
        assertEquals("smtp.example.com", config.getMailAccount().getSmtpHost());
        assertEquals("25", config.getMailAccount().getSmtpPort());
        assertEquals("email-ext", config.getMailAccount().getCredentialsId());
        assertEquals("@example.com", config.getDefaultSuffix());
        assertEquals("UTF-8", config.getCharset());
        assertEquals("emergency@example.com", config.getEmergencyReroute());
    }

    @Test
    void testFixEmptyAndTrimExtraSpaces() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        // add a credential to the GLOBAL scope
        UsernamePasswordCredentialsImpl c = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "email-ext", "Username/password for SMTP", "smtpUsername", "password");
        CredentialsProvider.lookupStores(j.jenkins).iterator().next().addCredentials(Domain.global(), c);

        MailAccount ma = new MailAccount();
        ma.setAddress("       example@example.com      ");
        ma.setSmtpHost("      smtp.example.com      ");
        ma.setSmtpPort("      25      ");
        ma.setCredentialsId("     email-ext     ");

        config.setMailAccount(ma);
        config.setDefaultSuffix("      @example.com      ");
        config.setCharset("      UTF-8      ");
        config.setEmergencyReroute("      emergency@example.com      ");
        try (JenkinsRule.WebClient client = createWebClient()) {
            j.submit(client.goTo("manage/email-ext/").getFormByName("config"));
        }

        assertEquals("example@example.com", config.getMailAccount().getAddress());
        assertEquals("smtp.example.com", config.getMailAccount().getSmtpHost());
        assertEquals("25", config.getMailAccount().getSmtpPort());
        assertEquals("email-ext", config.getMailAccount().getCredentialsId());
        assertEquals("@example.com", config.getDefaultSuffix());
        assertEquals("UTF-8", config.getCharset());
        assertEquals("emergency@example.com", config.getEmergencyReroute());
    }

    @Test
    void testFixEmptyAndTrimEmptyString() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        MailAccount ma = new MailAccount();
        ma.setAddress("");
        ma.setSmtpHost("");
        ma.setSmtpPort("");
        ma.setCredentialsId("");

        config.setMailAccount(ma);
        config.setDefaultSuffix("");
        config.setCharset("");
        config.setEmergencyReroute("");
        try (JenkinsRule.WebClient client = createWebClient()) {
            j.submit(client.goTo("manage/email-ext/").getFormByName("config"));
        }

        assertNull(config.getMailAccount().getAddress());
        assertNull(config.getMailAccount().getSmtpHost());
        assertEquals("25", config.getMailAccount().getSmtpPort());
        assertNull(config.getMailAccount().getCredentialsId());
        assertNull(config.getDefaultSuffix());
        assertEquals("UTF-8", config.getCharset());
        assertEquals("", config.getEmergencyReroute());
    }

    @Test
    void testFixEmptyAndTrimNull() throws Exception {
        ExtendedEmailGlobalConfiguration config = ExtendedEmailGlobalConfiguration.get();
        MailAccount ma = new MailAccount();
        ma.setAddress(null);
        ma.setSmtpHost(null);
        ma.setSmtpPort(null);
        ma.setCredentialsId(null);

        config.setMailAccount(ma);
        config.setDefaultSuffix(null);
        config.setCharset(null);
        config.setEmergencyReroute(null);
        try (JenkinsRule.WebClient client = createWebClient()) {
            j.submit(client.goTo("manage/email-ext/").getFormByName("config"));
        }

        assertNull(config.getMailAccount().getAddress());
        assertNull(config.getMailAccount().getSmtpHost());
        assertEquals("25", config.getMailAccount().getSmtpPort());
        assertNull(config.getMailAccount().getCredentialsId());
        assertNull(config.getDefaultSuffix());
        assertEquals("UTF-8", config.getCharset());
        assertEquals("", config.getEmergencyReroute());
    }

    @Test
    void emptyScriptApproval() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));
        try (JenkinsRule.WebClient client = createWebClient()) {
            HtmlPage page = client.login("admin").goTo("manage/email-ext/");
            HtmlForm form = page.getFormByName("config");
            j.submit(form);
        }
        assertThat(ScriptApproval.get().getPendingScripts(), is(empty()));
    }
}
