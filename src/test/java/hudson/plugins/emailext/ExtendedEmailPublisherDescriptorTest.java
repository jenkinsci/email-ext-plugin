package hudson.plugins.emailext;

/*
import hudson.Functions;
import jenkins.model.Jenkins;

import com.thoughtworks.selenium.DefaultSelenium;

import org.apache.commons.lang.StringUtils;
import static org.hamcrest.CoreMatchers.is;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assume.assumeThat;
import org.junit.Rule;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.JenkinsRule;

public class ExtendedEmailPublisherDescriptorTest extends HudsonTestCase {

    private DefaultSelenium selenium;
    @Rule
    public JenkinsRule rule = new JenkinsRule() {
        @Override
        protected void before() throws Throwable {
            assumeThat(System.getProperty("emailExtRunSeleniumTests", "false").equals("true"), is(false));
            super.before();
        }
    };

    protected DefaultSelenium createSeleniumClient(String url) throws Exception {
        String browserString = "*firefox";
        if (Functions.isWindows()) {
            browserString = "*iexplore";
        }
        return new DefaultSelenium("localhost", 4444, browserString, url);
    }

    public void testGlobalConfigDefaultState() throws Exception {
        // windows pretty much always has internet explorer which we use on there

        selenium = createSeleniumClient(getURL().toString());

        selenium.start();

        selenium.setTimeout("120000"); // wait up to 2 mins

        selenium.open("/configure");
        assertEquals("Should be at the Configure System page",
                "Configure System [Jenkins]", selenium.getTitle());

        // override global settings checkbox control
        assertTrue(selenium.isElementPresent("name=ext_mailer_override_global_settings"));
        assertFalse("Override global config should not be checked by default",
                selenium.isChecked("name=ext_mailer_override_global_settings"));

        // default content type select control
        assertTrue(selenium.isElementPresent("name=ext_mailer_default_content_type"));
        assertEquals("Plain text should be selected by default",
                "text/plain", selenium.getSelectedValue("name=ext_mailer_default_content_type"));

        assertTrue(selenium.isElementPresent("name=extmailer.useListID"));
        assertFalse("Use List ID should not be checked by default",
                selenium.isChecked("name=extmailer.useListID"));

        assertTrue(selenium.isElementPresent("name=extmailer.addPrecedenceBulk"));
        assertFalse("Add precedence bulk should not be checked by default",
                selenium.isChecked("name=extmailer.addPrecedenceBulk"));

        assertTrue(selenium.isElementPresent("name=ext_mailer_default_recipients"));
        assertEquals("Default recipients should be blank by default",
                "", selenium.getValue("name=ext_mailer_default_recipients"));

        assertTrue(selenium.isElementPresent("name=ext_mailer_default_replyto"));
        assertEquals("Default ReplyTo should be blank by default",
                "", selenium.getValue("name=ext_mailer_default_replyto"));

        assertTrue(selenium.isElementPresent("name=ext_mailer_emergency_reroute"));
        assertEquals("Emergency reroute should be blank by default",
                "", selenium.getValue("name=ext_mailer_emergency_reroute"));

        assertTrue(selenium.isElementPresent("name=ext_mailer_default_subject"));
        assertEquals("Default subject should be set by default",
                "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!",
                selenium.getValue("name=ext_mailer_default_subject"));

        assertTrue(selenium.isElementPresent("name=ext_mailer_max_attachment_size"));
        assertEquals("Max attachment size should be blank by default",
                "", selenium.getValue("name=ext_mailer_max_attachment_size"));

        assertTrue(selenium.isElementPresent("name=ext_mailer_default_body"));
        assertEquals("Default content should be set by default",
                "$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:\n\nCheck console output at $BUILD_URL to view the results.",
                selenium.getValue("name=ext_mailer_default_body"));

        assertTrue(selenium.isElementPresent("name=ext_mailer_debug_mode"));
        assertFalse("Debug mode should not be checked by default",
                selenium.isChecked("name=ext_mailer_debug_mode"));

        assertTrue(selenium.isElementPresent("name=ext_mailer_security_enabled"));
        assertFalse("Security mode should not be checked by default",
                selenium.isChecked("name=ext_mailer_security_enabled"));

        assertEquals("Content token help should be hidden by default",
                "display:none",
                StringUtils.replaceChars(selenium.getAttribute("id=contentTokenHelpConf@style"), " ;", ""));

        // assertTrue(selenium.isElementPresent("name=contentTokenAnchor"));

        // selenium.click("name=contentTokenAnchor");
        // assertEquals("Content token help should be shown when the help button is clicked", 
        //     "display:block", 
        //     StringUtils.replaceChars(selenium.getAttribute("id=contentTokenHelpConf@style"), " ;", ""));

        selenium.stop();
    }
}
*/