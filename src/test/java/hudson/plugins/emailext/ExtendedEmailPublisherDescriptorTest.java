package hudson.plugins.emailext;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

import org.jvnet.hudson.test.Bug;

public class ExtendedEmailPublisherDescriptorTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testGlobalConfigDefaultState() throws Exception {
        HtmlPage page = j.createWebClient().goTo("configure");

        assertEquals("Should be at the Configure System page",
                "Configure System [Jenkins]", page.getTitleText());

        // override global settings checkbox control                
        HtmlCheckBoxInput overrideGlobal = page.getElementByName("ext_mailer_override_global_settings");
        assertNotNull("Override global settings should be present", overrideGlobal);
        assertFalse("Override global config should not be checked by default", overrideGlobal.isChecked());

        // default content type select control
        HtmlSelect contentType = page.getElementByName("ext_mailer_default_content_type");
        assertNotNull("Content type selection should be present", contentType);
        assertEquals("Plain text should be selected by default",
                "text/plain", contentType.getSelectedOptions().get(0).getValueAttribute());

        HtmlCheckBoxInput useListId = page.getElementByName("ext_mailer_use_list_id");
        assertNotNull("Use List ID should be present", useListId);
        assertFalse("Use List ID should not be checked by default", useListId.isChecked());

        HtmlCheckBoxInput precedenceBulk = page.getElementByName("ext_mailer_add_precedence_bulk");
        assertNotNull("Precedence Bulk should be present", precedenceBulk);
        assertFalse("Add precedence bulk should not be checked by default",
                precedenceBulk.isChecked());

        HtmlTextInput defaultRecipients = page.getElementByName("ext_mailer_default_recipients");
        assertNotNull("Default Recipients should be present", defaultRecipients);
        assertEquals("Default recipients should be blank by default", 
                "", defaultRecipients.getText());

        HtmlTextInput defaultReplyTo = page.getElementByName("ext_mailer_default_replyto");
        assertNotNull("Default Reply-to should be present", defaultReplyTo);
        assertEquals("Default Reply-To should be blank by default", 
                "", defaultReplyTo.getText());

        HtmlTextInput emergencyReroute = page.getElementByName("ext_mailer_emergency_reroute");
        assertNotNull("Emergency Reroute should be present", emergencyReroute);
        assertEquals("Emergency Reroute should be blank by default", 
                "", emergencyReroute.getText());
        
        HtmlTextInput excludedRecipients = page.getElementByName("ext_mailer_excluded_committers");
        assertNotNull("Excluded Recipients should be present", excludedRecipients);
        assertEquals("Excluded Recipients should be blank by default",
                "", excludedRecipients.getText());

        HtmlTextInput defaultSubject = page.getElementByName("ext_mailer_default_subject");
        assertNotNull("Default Subject should be present", defaultSubject);
        assertEquals("Default Subject should be set", 
                "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!", 
                defaultSubject.getText());
        
        HtmlTextInput maxAttachmentSize = page.getElementByName("ext_mailer_max_attachment_size");
        assertNotNull("Max attachment size should be present", maxAttachmentSize);
        assertEquals("Max attachment size should be blank by default",
                "", maxAttachmentSize.getText());
        
        HtmlTextArea defaultContent = page.getElementByName("ext_mailer_default_body");
        assertNotNull("Default content should be present", defaultContent);
        assertEquals("Default content should be set by default",
                "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:\n\nCheck console output at $BUILD_URL to view the results.",
                defaultContent.getText());
        
        HtmlCheckBoxInput debugMode = page.getElementByName("ext_mailer_debug_mode");
        assertNotNull("Debug mode should be present", debugMode);
        assertFalse("Debug mode should not be checked by default", debugMode.isChecked());
        
        HtmlCheckBoxInput securityMode = page.getElementByName("ext_mailer_security_enabled");
        assertNotNull("Security mode should be present", securityMode);
        assertFalse("Security mode should not be checked by default", securityMode.isChecked());
        
        try {
            page.getElementByName("defaultClasspath");
            fail("defaultClasspath section should not be present");
        } catch (ElementNotFoundException e) {}
    }
    
    @Test
    @Bug(20030)
    public void testGlobalConfigSimpleRoundTrip() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);        
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlTextInput defaultRecipients = page.getElementByName("ext_mailer_default_recipients");
        defaultRecipients.setValueAttribute("mickey@disney.com");
        j.submit(page.getFormByName("config"));       
        
        assertEquals("mickey@disney.com", descriptor.getDefaultRecipients());
    }

    @Test
    @Bug(20133)
    public void testPrecedenceBulkSettingRoundTrip() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlCheckBoxInput addPrecedenceBulk = page.getElementByName("ext_mailer_add_precedence_bulk");
        addPrecedenceBulk.setChecked(true);
        j.submit(page.getFormByName("config"));

        assertEquals(true, descriptor.getPrecedenceBulk());
    }

    @Test
    @Bug(20133)
    public void testListIDRoundTrip() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor = j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlCheckBoxInput useListId = page.getElementByName("ext_mailer_use_list_id");
        useListId.setChecked(true);
        HtmlTextInput listId = page.getElementByName("ext_mailer_list_id");
        listId.setValueAttribute("hammer");

        j.submit(page.getFormByName("config"));

        assertEquals("hammer", descriptor.getListId());
    }
}
