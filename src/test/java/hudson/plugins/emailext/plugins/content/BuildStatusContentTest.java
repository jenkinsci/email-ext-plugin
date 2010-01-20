package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;                     

@SuppressWarnings({"unchecked"})
public class BuildStatusContentTest {

    @Test
    public void testGetContent_whenBuildIsBuildingThenStatusShouldBeBuilding() {
        // Test for HUDSON-953
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.isBuilding()).thenReturn(true);

        String content = new BuildStatusContent().getContent(build, null, null, null);

        assertEquals("Building", content);
    }
}
