package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.util.StreamTaskListener;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
public class BuildStatusContentTest {

    @Test
    public void testGetContent_whenBuildIsBuildingThenStatusShouldBeBuilding() 
        throws Exception {
        // Test for HUDSON-953
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.isBuilding()).thenReturn(true);

        String content = new BuildStatusContent().evaluate(build, build.getWorkspace(), StreamTaskListener.fromStdout(), BuildStatusContent.MACRO_NAME);

        assertEquals("Building", content);
    }
}
