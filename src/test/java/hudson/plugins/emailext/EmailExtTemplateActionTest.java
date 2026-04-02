package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class EmailExtTemplateActionTest {

    @Test
    void testRenderTemplateInterruptedExceptionRestoresInterruptFlag(JenkinsRule j)
            throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        EmailExtTemplateAction action = new EmailExtTemplateAction(project);

        Thread.currentThread().interrupt();

        String[] result = action.renderTemplate("nonexistent.template", "1");

        assertTrue(
            Thread.interrupted(),
            "Interrupt flag should be restored after InterruptedException"
        );
    }
}