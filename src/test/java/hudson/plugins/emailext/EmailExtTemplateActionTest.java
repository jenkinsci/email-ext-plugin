package hudson.plugins.emailext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTextInput;
import org.htmlunit.html.HtmlSelect;
import org.htmlunit.html.HtmlForm;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test for EmailExtTemplateAction CSP compliance and functionality.
 */
public class EmailExtTemplateActionTest {

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    @Issue("JENKINS-74891")
    public void testTemplateActionPageRendersCorrectly() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-project");
        
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.defaultSubject = "Test Subject";
        publisher.defaultContent = "Test Content";
        project.getPublishersList().add(publisher);
        
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        
        JenkinsRule.WebClient webClient = j.createWebClient();
        webClient.getOptions().setJavaScriptEnabled(true);
        
        HtmlPage page = webClient.goTo(project.getUrl() + "templateTest");
        
        assertNotNull("Page should not be null", page);
        assertTrue("Page should contain template test form", 
                   page.asNormalizedText().contains("Jelly/Groovy Template File Name"));
        
        HtmlForm form = page.getFormByName("templateTest");
        assertNotNull("Template test form should exist", form);
        
        HtmlTextInput templateFileInput = form.getInputByName("template_file_name");
        assertNotNull("Template file name input should exist", templateFileInput);
        
        HtmlSelect buildSelect = form.getSelectByName("template_build");
        assertNotNull("Build select should exist", buildSelect);
    }

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
        
        assertTrue("Page should not contain inline onclick handlers", 
                   !pageContent.contains("onclick=\""));
        assertTrue("Page should not contain inline onsubmit handlers", 
                   !pageContent.contains("onsubmit=\""));
    }

    @Test
    @Issue("JENKINS-74891")
    public void testRenderTemplateMethod() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-project");
        
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        publisher.defaultSubject = "Test Subject";
        publisher.defaultContent = "Test Content";
        project.getPublishersList().add(publisher);
        
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);
        
        String[] result = action.renderTemplate("text.txt", build.getId());
        
        assertNotNull("Result should not be null", result);
        assertEquals("Result should have 2 elements", 2, result.length);
        assertNotNull("Rendered content should not be null", result[0]);
        assertNotNull("Console output should not be null", result[1]);
    }

    @Test
    @Issue("JENKINS-74891")
    public void testTemplateFileCheckValidation() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-project");
        
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        project.getPublishersList().add(publisher);
        
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);
        
        hudson.util.FormValidation validation = action.doTemplateFileCheck("text.txt");
        
        assertNotNull("Validation should not be null", validation);
    }

    @Test
    public void testActionProperties() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-project");
        
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);
        
        assertEquals("URL name should be 'templateTest'", "templateTest", action.getUrlName());
        assertNotNull("Display name should not be null", action.getDisplayName());
        assertEquals("Project should match", project, action.getProject());
    }
}
