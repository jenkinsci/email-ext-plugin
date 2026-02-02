package hudson.plugins.emailext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test for EmailExtTemplateAction CSP compliance.
 */
public class EmailExtTemplateActionTest {

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    @Issue("JENKINS-74891")
    public void testJavaScriptNoInlineScripts() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-project");
        
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.defaultSubject = "Test Subject";
        publisher.defaultContent = "Test Content";
        project.getPublishersList().add(publisher);
        
        j.buildAndAssertSuccess(project);
        
        JenkinsRule.WebClient webClient = j.createWebClient();
        webClient.getOptions().setJavaScriptEnabled(false);
        
        HtmlPage page = webClient.goTo(project.getUrl() + "templateTest");
        
        String pageContent = page.asXml();
        
        assertNotNull("Page content should not be null", pageContent);
        
        assertFalse("Page should not contain inline onclick handlers", 
                    pageContent.contains("onclick=\""));
        assertFalse("Page should not contain inline onsubmit handlers", 
                    pageContent.contains("onsubmit=\""));
    }

    @Test
    @Issue("JENKINS-74891")
    public void testJavaScriptFileIsCSPCompliant() throws Exception {
        java.io.InputStream is = getClass().getClassLoader()
            .getResourceAsStream("hudson/plugins/emailext/EmailExtTemplateAction/template-test.js");
        assertNotNull("JavaScript file should exist", is);
        
        String jsContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        
        assertFalse("JavaScript should not use innerHTML (CSP violation)", 
                    jsContent.contains("innerHTML"));
        
        assertFalse("JavaScript should not use deprecated escape() function", 
                    jsContent.contains("escape("));
        
        assertTrue("JavaScript should use textContent instead of innerHTML", 
                   jsContent.contains("textContent"));
        
        assertTrue("JavaScript should use encodeURIComponent instead of escape", 
                   jsContent.contains("encodeURIComponent"));
    }
}
