package hudson.plugins.emailext;

import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.jvnet.hudson.test.HudsonTestCase;

public class ExtendedEmailPublisherDescriptorTest extends HudsonTestCase {

    public void testGlobalConfigDefaultState() throws Exception {
        HtmlPage page = createWebClient().goTo("configure");

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

        HtmlCheckBoxInput useListId = page.getElementByName("extmailer.useListID");
        assertNotNull("Use List ID should be present", useListId);
        assertFalse("Use List ID should not be checked by default", useListId.isChecked());

        HtmlCheckBoxInput precedenceBulk = page.getElementByName("extmailer.addPrecedenceBulk");
        WebAssert.notNull("Precedence Bulk should be present", precedenceBulk);
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
//        
//        HtmlCheckBoxInput securityMode = page.getElementByName("ext_mailer_security_enabled");
//        assertNotNull("Security mode should be present", securityMode);
//        assertFalse("Security mode should not be checked by default", securityMode.isChecked());
    }
}
