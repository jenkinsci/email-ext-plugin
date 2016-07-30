package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.analysis.core.AbstractResultAction;
import hudson.plugins.analysis.core.MavenResultAction;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;

public class StaticAnalysisUtilitiesTest {

    @Test
    public void getActions() throws Exception {
        final StaticAnalysisUtilities utilities = new StaticAnalysisUtilities();
        AbstractBuild build = new FreeStyleBuild(mock(FreeStyleProject.class));
        Action action = mock(Action.class);
        build.addAction(action);
        assertThat(utilities.getActions(build), Matchers.hasSize(0));

        build = new FreeStyleBuild(mock(FreeStyleProject.class));
        action = mock(AbstractResultAction.class);
        build.addAction(action);
        assertThat(utilities.getActions(build), hasItem(action));

        build = new FreeStyleBuild(mock(FreeStyleProject.class));
        action = mock(MavenResultAction.class);
        build.addAction(action);
        assertThat(utilities.getActions(build), hasItem(action));
    }

}
