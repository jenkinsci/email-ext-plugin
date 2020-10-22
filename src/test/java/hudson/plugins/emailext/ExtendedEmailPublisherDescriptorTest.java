package hudson.plugins.emailext;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlNumberInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import hudson.Functions;
import hudson.model.Descriptor;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.Permission;
import hudson.util.ReflectionUtils;
import jenkins.model.Jenkins;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.mail.Authenticator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExtendedEmailPublisherDescriptorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testGlobalConfigDefaultState() throws Exception {
        HtmlPage page = j.createWebClient().goTo("configure");

        assertEquals("Should be at the Configure System page",
                "Configure System [Jenkins]", page.getTitleText());

        HtmlTextInput smtpHost = page.getElementByName("_.smtpHost");
        assertNotNull("SMTP Server should be present", smtpHost);
        assertEquals("SMTP Server should be blank by default", "", smtpHost.getText());

        HtmlNumberInput smtpPort = page.getElementByName("_.smtpPort");
        assertNotNull("SMTP Port should be present", smtpPort);
        assertEquals("SMTP Port should be 25 by default", "25", smtpPort.getText());

        HtmlTextInput defaultSuffix = page.getElementByName("_.defaultSuffix");
        assertNotNull("Default suffix should be present", defaultSuffix);
        assertEquals("Default suffix should be blank by default", "", defaultSuffix.getText());

        // default content type select control
        HtmlSelect contentType = page.getElementByName("_.defaultContentType");
        assertNotNull("Content type selection should be present", contentType);
        assertEquals("Plain text should be selected by default",
                "text/plain", contentType.getSelectedOptions().get(0).getValueAttribute());

        HtmlCheckBoxInput precedenceBulk = page.getElementByName("_.precedenceBulk");
        assertNotNull("Precedence Bulk should be present", precedenceBulk);
        assertFalse("Add precedence bulk should not be checked by default",
                precedenceBulk.isChecked());

        HtmlTextInput defaultRecipients = page.getElementByName("_.defaultRecipients");
        assertNotNull("Default Recipients should be present", defaultRecipients);
        assertEquals("Default recipients should be blank by default",
                "", defaultRecipients.getText());

        HtmlTextInput defaultReplyTo = page.getElementByName("_.defaultReplyTo");
        assertNotNull("Default Reply-to should be present", defaultReplyTo);
        assertEquals("Default Reply-To should be blank by default",
                "", defaultReplyTo.getText());

        HtmlTextInput emergencyReroute = page.getElementByName("_.emergencyReroute");
        assertNotNull("Emergency Reroute should be present", emergencyReroute);
        assertEquals("Emergency Reroute should be blank by default",
                "", emergencyReroute.getText());

        HtmlTextInput allowedDomains = page.getElementByName("_.allowedDomains");
        assertNotNull("Allowed Domains should be present", allowedDomains);
        assertEquals("Allowed Domains should be blank by default",
                "", allowedDomains.getText());

        HtmlTextInput excludedRecipients = page.getElementByName("_.excludedCommitters");
        assertNotNull("Excluded Recipients should be present", excludedRecipients);
        assertEquals("Excluded Recipients should be blank by default",
                "", excludedRecipients.getText());

        HtmlTextInput defaultSubject = page.getElementByName("_.defaultSubject");
        assertNotNull("Default Subject should be present", defaultSubject);
        assertEquals("Default Subject should be set",
                "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!",
                defaultSubject.getText());

        HtmlNumberInput maxAttachmentSize = page.getElementByName("_.maxAttachmentSizeMb");
        assertNotNull("Max attachment size should be present", maxAttachmentSize);
        assertThat("Max attachment size should be blank or -1 by default", maxAttachmentSize.getText(), is(oneOf("", "-1")));

        HtmlTextArea defaultContent = page.getElementByName("_.defaultBody");
        assertNotNull("Default content should be present", defaultContent);
        assertEquals("Default content should be set by default",
                "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:\n\nCheck console output at $BUILD_URL to view the results.",
                defaultContent.getText());

        HtmlTextArea defaultPresendScript = page.getElementByName("_.defaultPresendScript");
        assertNotNull("Default presend script should be present", defaultPresendScript);
        assertEquals("Default presend script should be blank by default", "", defaultPresendScript.getText());

        HtmlTextArea defaultPostsendScript = page.getElementByName("_.defaultPostsendScript");
        assertNotNull("Default postsend script should be present", defaultPostsendScript);
        assertEquals("Default postsend script should be blank by default", "", defaultPostsendScript.getText());

        HtmlCheckBoxInput debugMode = page.getElementByName("_.debugMode");
        assertNotNull("Debug mode should be present", debugMode);
        assertFalse("Debug mode should not be checked by default", debugMode.isChecked());

        HtmlCheckBoxInput adminRequiredForTemplateTesting = page.getElementByName("_.adminRequiredForTemplateTesting");
        assertNotNull("Admin required for template testing should be present", adminRequiredForTemplateTesting);
        assertFalse("Admin required for template testing should be unchecked by default", adminRequiredForTemplateTesting.isChecked());

        HtmlCheckBoxInput watchingEnabled = page.getElementByName("_.watchingEnabled");
        assertNotNull("Watching enable should be present", watchingEnabled);
        assertFalse("Watching enable should be unchecked by default", watchingEnabled.isChecked());

        HtmlCheckBoxInput allowUnregisteredEnabled = page.getElementByName("_.allowUnregisteredEnabled");
        assertNotNull("Allow unregistered should be present", allowUnregisteredEnabled);
        assertFalse("Allow unregistered should be unchecked by default", allowUnregisteredEnabled.isChecked());

        try {
            page.getElementByName("defaultClasspath");
            fail("defaultClasspath section should not be present");
        } catch (ElementNotFoundException e) {}
    }

    @Test
    @Issue("JENKINS-20030")
    public void testGlobalConfigSimpleRoundTrip() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlTextInput defaultRecipients = page.getElementByName("_.defaultRecipients");
        defaultRecipients.setValueAttribute("mickey@disney.com");
        j.submit(page.getFormByName("config"));

        assertEquals("mickey@disney.com", descriptor.getDefaultRecipients());
    }

    @Test
    @Issue("JENKINS-63367")
    public void testSmtpPortRetainsSetValue() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        JenkinsRule.WebClient client = j.createWebClient();
        HtmlPage page = client.goTo("configure");
        HtmlNumberInput smtpPort = page.getElementByName("_.smtpPort");
        smtpPort.setValueAttribute("587");
        j.submit(page.getFormByName("config"));

        assertEquals("587", descriptor.getMailAccount().getSmtpPort());

        page = client.goTo("configure");
        smtpPort = page.getElementByName("_.smtpPort");
        assertEquals("587", smtpPort.getValueAttribute());
    }

    @Test
    @Issue("JENKINS-20133")
    public void testPrecedenceBulkSettingRoundTrip() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlCheckBoxInput addPrecedenceBulk = page.getElementByName("_.precedenceBulk");
        addPrecedenceBulk.setChecked(true);
        j.submit(page.getFormByName("config"));

        assertEquals(true, descriptor.getPrecedenceBulk());
    }

    @Test
    @Issue("JENKINS-20133")
    public void testListIDRoundTrip() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlTextInput listId = page.getElementByName("_.listId");
        listId.setValueAttribute("hammer");

        j.submit(page.getFormByName("config"));

        assertEquals("hammer", descriptor.getListId());
    }

    @Test
    public void testAdvancedProperties() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlTextArea advProperties = page.getElementByName("_.advProperties");
        advProperties.setText("mail.smtp.starttls.enable=true");
        j.submit(page.getFormByName("config"));

        assertEquals("mail.smtp.starttls.enable=true", descriptor.getAdvProperties());
    }

    @Test
    public void defaultTriggers() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");

        assertEquals("Should be at the Configure System page",
                "Configure System [Jenkins]", page.getTitleText());

        List<DomElement> settings = page.getByXPath(".//div[@class='advancedLink' and span[starts-with(@id, 'yui-gen')]/span[@class='first-child']/button[./text()='Default Triggers...']]");
        assertTrue(settings.size() == 1);
        DomNode div = settings.get(0);
        DomNode table = div.getNextSibling();
        assertTrue(table.getLocalName().equals("table"));
        DomNode tbody = table.getFirstChild();
        assertTrue(tbody.getLocalName().equals("tbody"));
        assertFalse(tbody.getChildNodes().isEmpty());

        List<DomNode> nodes = div.getByXPath(".//button[./text()='Default Triggers...']");
        assertTrue(nodes.size() == 1);
        HtmlButton defaultTriggers = (HtmlButton)nodes.get(0);
        defaultTriggers.click();

        String[] selectedTriggers = {
                "hudson.plugins.emailext.plugins.trigger.AbortedTrigger",
                "hudson.plugins.emailext.plugins.trigger.PreBuildTrigger",
                "hudson.plugins.emailext.plugins.trigger.FixedTrigger",
                "hudson.plugins.emailext.plugins.trigger.RegressionTrigger",
        };

        List<DomNode> failureTrigger = page.getByXPath(".//input[@json='hudson.plugins.emailext.plugins.trigger.FailureTrigger']");
        assertTrue(failureTrigger.size() == 1);
        HtmlCheckBoxInput failureTriggerCheckBox = (HtmlCheckBoxInput)failureTrigger.get(0);
        assertTrue(failureTriggerCheckBox.isChecked());
        failureTriggerCheckBox.setChecked(false);

        for(String selectedTrigger : selectedTriggers) {
            List<DomNode> triggerItems = page.getByXPath(".//input[@name='_.defaultTriggerIds' and @json='"+selectedTrigger+"']");
            assertEquals(1, triggerItems.size());
            HtmlCheckBoxInput checkBox = (HtmlCheckBoxInput)triggerItems.get(0);
            checkBox.setChecked(true);
        }

        j.submit(page.getFormByName("config"));
        assertArrayEquals(selectedTriggers, descriptor.getDefaultTriggerIds().toArray(new String[0]));
    }

    @Test
    public void groovyClassPath() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");

        assertEquals("Should be at the Configure System page",
                "Configure System [Jenkins]", page.getTitleText());

        List<DomElement> nodes = page.getByXPath(".//td[@class='setting-name' and ./text()='Additional groovy classpath']");
        assertEquals(1, nodes.size());
        HtmlTableCell settingName = (HtmlTableCell)nodes.get(0);

        nodes = settingName.getByXPath("../td[@class='setting-main']/div[@class='repeated-container']/div[@name='defaultClasspath']");
        assertTrue("Should not have any class path setup by default", nodes.size() == 0);

        nodes = settingName.getByXPath("../td[@class='setting-main']/div[@class='repeated-container' and span[starts-with(@id, 'yui-gen')]/span[@class='first-child']/button[./text()='Add']]");
        assertEquals(1, nodes.size());
        HtmlDivision div = (HtmlDivision)nodes.get(0);
        nodes = div.getByXPath(".//button[./text()='Add']");
        HtmlButton addButton = (HtmlButton)nodes.get(0);
        addButton.click();

        nodes = settingName.getByXPath("../td[@class='setting-main']/div[@class='repeated-container']/div[@name='defaultClasspath']");
        assertTrue(nodes.size() == 1);
        div = (HtmlDivision) nodes.get(0);
        String divClass = div.getAttribute("class");
        assertTrue(divClass.contains("first") && divClass.contains("last") && divClass.contains("only"));

        nodes = div.getByXPath(".//input[@name='_.path' and @type='text']");
        assertEquals(1, nodes.size());

        HtmlTextInput path = (HtmlTextInput)nodes.get(0);
        path.setText("/path/to/classes");

        addButton.click();

        nodes = settingName.getByXPath("../td[@class='setting-main']/div[@class='repeated-container']/div[@name='defaultClasspath' and contains(@class, 'last')]");
        assertTrue(nodes.size() == 1);
        div = (HtmlDivision) nodes.get(0);
        divClass = div.getAttribute("class");
        assertTrue(divClass.contains("last"));
        assertFalse(divClass.contains("first") || divClass.contains("only"));

        nodes = div.getByXPath(".//input[@name='_.path' and @type='text']");
        assertEquals(1, nodes.size());
        path = (HtmlTextInput)nodes.get(0);
        path.setText("/other/path/to/classes");

        j.submit(page.getFormByName("config"));

        String[] classpath = {
                "/path/to/classes",
                "/other/path/to/classes",
        };

        assertArrayEquals(classpath, descriptor.getDefaultClasspath()
                .stream()
                .map(GroovyScriptPath::getPath)
                .toArray(String[]::new));
    }

    @Test
    public void managePermissionShouldAccess() {
        Permission jenkinsManage;
        try {
            jenkinsManage = getJenkinsManage();
        } catch (Exception e) {
            Assume.assumeTrue("Jenkins baseline is too old for this test (requires Jenkins.MANAGE)", false);
            return;
        }
        final String USER = "user";
        final String MANAGER = "manager";
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                // Read access
                .grant(Jenkins.READ).everywhere().to(USER)

                // Read and Manage
                .grant(Jenkins.READ).everywhere().to(MANAGER)
                .grant(jenkinsManage).everywhere().to(MANAGER)
        );
        try (ACLContext c = ACL.as(User.getById(USER, true))) {
            Collection<Descriptor> descriptors = Functions.getSortedDescriptorsForGlobalConfigUnclassified();
            assertTrue("Global configuration should not be accessible to READ users", descriptors.size() == 0);
        }
        try (ACLContext c = ACL.as(User.getById(MANAGER, true))) {
            Collection<Descriptor> descriptors = Functions.getSortedDescriptorsForGlobalConfigUnclassified();
            Optional<Descriptor> found = descriptors.stream().filter(descriptor -> descriptor instanceof ExtendedEmailPublisherDescriptor).findFirst();
            assertTrue("Global configuration should be accessible to MANAGE users", found.isPresent());
        }
    }

    // TODO: remove when Jenkins core baseline is 2.222+
    private Permission getJenkinsManage() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // Jenkins.MANAGE is available starting from Jenkins 2.222 (https://jenkins.io/changelog/#v2.222). See JEP-223 for more info
        return (Permission) ReflectionUtils.getPublicProperty(Jenkins.get(), "MANAGE");
    }

    @Test
    @Issue("JENKINS-63311")
    public void authenticatorIsCreatedWhenUsernameAndPasswordAreFilledOut() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        String from = "test@example.com";
        MailAccount ma = new MailAccount();
        ma.setAddress(from);
        ma.setSmtpHost("smtp.example.com");
        ma.setSmtpPort("25");
        ma.setSmtpUsername("mail_user");
        ma.setSmtpPassword("smtpPassword");
        assertTrue(ma.isValid());
        descriptor.setAddAccounts(Collections.singletonList(ma));
        Function<MailAccount, Authenticator> authenticatorProvider = Mockito.mock(Function.class);
        descriptor.setAuthenticatorProvider(authenticatorProvider);
        descriptor.createSession(from);
        ArgumentCaptor<MailAccount> mailAccountCaptor = ArgumentCaptor.forClass(MailAccount.class);
        Mockito.verify(authenticatorProvider, Mockito.times(1)).apply(mailAccountCaptor.capture());
        assertNotNull(mailAccountCaptor.getValue());
    }

    @Test
    @Issue("JENKINS-63311")
    public void authenticatorIsCreatedWhenUsernameIsFilledOutButPasswordIsNull() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        String from = "test@example.com";
        MailAccount ma = new MailAccount();
        ma.setAddress(from);
        ma.setSmtpHost("smtp.example.com");
        ma.setSmtpPort("25");
        ma.setSmtpUsername("mail_user");
        ma.setSmtpPassword((String) null);
        descriptor.setAddAccounts(Collections.singletonList(ma));
        Function<MailAccount, Authenticator> authenticatorProvider = Mockito.mock(Function.class);
        descriptor.setAuthenticatorProvider(authenticatorProvider);
        descriptor.createSession(from);
        ArgumentCaptor<MailAccount> mailAccountCaptor = ArgumentCaptor.forClass(MailAccount.class);
        Mockito.verify(authenticatorProvider, Mockito.times(1)).apply(mailAccountCaptor.capture());
        assertNotNull(mailAccountCaptor.getValue());
    }

    @Test
    @Issue("JENKINS-63311")
    public void noAuthenticatorIsCreatedWhenUsernameIsBlank() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        String from = "test@example.com";
        MailAccount ma = new MailAccount();
        ma.setAddress(from);
        ma.setSmtpHost("smtp.example.com");
        ma.setSmtpPort("25");
        ma.setSmtpUsername(" ");
        ma.setSmtpPassword("smtpPassword");
        descriptor.setAddAccounts(Collections.singletonList(ma));
        Function<MailAccount, Authenticator> authenticatorProvider = Mockito.mock(Function.class);
        descriptor.setAuthenticatorProvider(authenticatorProvider);
        descriptor.createSession(from);
        ArgumentCaptor<MailAccount> mailAccountCaptor = ArgumentCaptor.forClass(MailAccount.class);
        Mockito.verify(authenticatorProvider, Mockito.never()).apply(mailAccountCaptor.capture());
    }

    @Test
    public void testFixEmptyAndTrimNormal() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        MailAccount ma = new MailAccount();
        ma.setAddress("example@example.com");
        ma.setSmtpHost("smtp.example.com");
        ma.setSmtpPort("25");
        ma.setSmtpUsername("smtpUsername");

        descriptor.setMailAccount(ma);
        descriptor.setDefaultSuffix("@example.com");
        descriptor.setCharset("UTF-8");
        descriptor.setEmergencyReroute("emergency@example.com");
        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        assertEquals("example@example.com",descriptor.getMailAccount().getAddress());
        assertEquals("smtp.example.com",descriptor.getMailAccount().getSmtpHost());
        assertEquals("25",descriptor.getMailAccount().getSmtpPort());
        assertEquals("smtpUsername",descriptor.getMailAccount().getSmtpUsername());
        assertEquals("@example.com",descriptor.getDefaultSuffix());
        assertEquals("UTF-8",descriptor.getCharset());
        assertEquals("emergency@example.com",descriptor.getEmergencyReroute());
    }

    @Test
    public void testFixEmptyAndTrimExtraSpaces() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        MailAccount ma = new MailAccount();
        ma.setAddress("       example@example.com      ");
        ma.setSmtpHost("      smtp.example.com      ");
        ma.setSmtpPort("      25      ");
        ma.setSmtpUsername("      smtpUsername      ");

        descriptor.setMailAccount(ma);
        descriptor.setDefaultSuffix("      @example.com      ");
        descriptor.setCharset("      UTF-8      ");
        descriptor.setEmergencyReroute("      emergency@example.com      ");
        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        assertEquals("example@example.com",descriptor.getMailAccount().getAddress());
        assertEquals("smtp.example.com",descriptor.getMailAccount().getSmtpHost());
        assertEquals("25",descriptor.getMailAccount().getSmtpPort());
        assertEquals("smtpUsername",descriptor.getMailAccount().getSmtpUsername());
        assertEquals("@example.com",descriptor.getDefaultSuffix());
        assertEquals("UTF-8",descriptor.getCharset());
        assertEquals("emergency@example.com",descriptor.getEmergencyReroute());
    }

    @Test
    public void testFixEmptyAndTrimEmptyString() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        MailAccount ma = new MailAccount();
        ma.setAddress("");
        ma.setSmtpHost("");
        ma.setSmtpPort("");
        ma.setSmtpUsername("");

        descriptor.setMailAccount(ma);
        descriptor.setDefaultSuffix("");
        descriptor.setCharset("");
        descriptor.setEmergencyReroute("");
        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        assertEquals("address not configured yet <nobody@nowhere>",descriptor.getMailAccount().getAddress());
        assertNull(descriptor.getMailAccount().getSmtpHost());
        assertEquals("25",descriptor.getMailAccount().getSmtpPort());
        assertNull(descriptor.getMailAccount().getSmtpUsername());
        assertNull(descriptor.getDefaultSuffix());
        assertEquals("UTF-8",descriptor.getCharset());
        assertEquals("",descriptor.getEmergencyReroute());
    }

    @Test
    public void testFixEmptyAndTrimNull() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        MailAccount ma = new MailAccount();
        ma.setAddress(null);
        ma.setSmtpHost(null);
        ma.setSmtpPort(null);
        ma.setSmtpUsername(null);

        descriptor.setMailAccount(ma);
        descriptor.setDefaultSuffix(null);
        descriptor.setCharset(null);
        descriptor.setEmergencyReroute(null);
        j.submit(j.createWebClient().goTo("configure").getFormByName("config"));

        assertEquals("address not configured yet <nobody@nowhere>",descriptor.getMailAccount().getAddress());
        assertNull(descriptor.getMailAccount().getSmtpHost());
        assertEquals("25",descriptor.getMailAccount().getSmtpPort());
        assertNull(descriptor.getMailAccount().getSmtpUsername());
        assertNull(descriptor.getDefaultSuffix());
        assertEquals("UTF-8",descriptor.getCharset());
        assertEquals("",descriptor.getEmergencyReroute());
    }
}
