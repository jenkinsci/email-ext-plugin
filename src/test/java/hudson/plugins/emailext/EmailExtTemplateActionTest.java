package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class EmailExtTemplateActionTest {

    @Test
    void testRenderErrorShowsErrorHeading(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        String[] result = action.renderTemplate("nonexistent.groovy", "invalid-build-id");

        assertTrue(result[0].contains("<h3>An error occurred"),
            "Should contain error heading from renderError()");
    }

    @Test
    void testRenderErrorShowsRedSpan(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        String[] result = action.renderTemplate("nonexistent.groovy", "invalid-build-id");

        assertTrue(result[0].contains("<span style=\"color:red"),
            "Should contain red span from renderError()");
    }

    @Test
    void testRenderErrorNewlineConvertedToBr(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        String[] result = action.renderTemplate("nonexistent.groovy", "invalid-build-id");

        assertTrue(result[0].contains("<br/>"),
            "Newlines should be converted to <br/> by renderError()");
    }
}