package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class EmailExtTemplateActionFactoryTest {

    @Test
    void testCreateForReturnsEmptyListWhenNoEmailExt(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        EmailExtTemplateActionFactory factory = new EmailExtTemplateActionFactory();
        assertTrue(factory.createFor(project).isEmpty());
    }

    @Test
    void testCreateForReturnsActionWhenEmailExtPresent(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getPublishersList().add(new ExtendedEmailPublisher());
        EmailExtTemplateActionFactory factory = new EmailExtTemplateActionFactory();
        assertEquals(1, factory.createFor(project).size());
    }
}
