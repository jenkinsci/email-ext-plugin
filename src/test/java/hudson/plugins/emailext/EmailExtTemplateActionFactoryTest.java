package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.util.DescribableList;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;

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

        Collection<? extends Action> actions = factory.createFor(project);

        assertEquals(1, actions.size());
        Action action = actions.iterator().next();
        EmailExtTemplateAction templateAction = assertInstanceOf(EmailExtTemplateAction.class, action);
        assertEquals(project, templateAction.getProject());
    }

    @Test
    void testCreateForReturnsEmptyListWhenPublishersListIsNull() {
        FreeStyleProject project = Mockito.mock(FreeStyleProject.class);
        Mockito.doReturn((DescribableList<?, ?>) null).when(project).getPublishersList();

        EmailExtTemplateActionFactory factory = new EmailExtTemplateActionFactory();

        assertTrue(factory.createFor(project).isEmpty());
    }
}
