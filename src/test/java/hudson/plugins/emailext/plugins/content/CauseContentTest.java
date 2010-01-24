package hudson.plugins.emailext.plugins.content;

import hudson.model.Build;
import hudson.model.Cause;
import hudson.model.CauseAction;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
public class CauseContentTest {
    private CauseContent causeContent;

    private Build build;

    @Before
    public void setUp() {
        causeContent = new CauseContent();

        build = mock(Build.class);
    }

    @Test
    public void shouldReturnNA_whenNoCauseActionIsFound() {
        when(build.getAction(CauseAction.class)).thenReturn(null);

        assertEquals("N/A", causeContent.getContent(build, null, null, null));
    }

    @Test
    public void shouldReturnNA_whenThereIsNoCause() {
        CauseAction causeAction = mock(CauseAction.class);
        when(build.getAction(CauseAction.class)).thenReturn(causeAction);

        assertEquals("N/A", causeContent.getContent(build, null, null, null));
    }

    @Test
    public void shouldReturnSingleCause() {
        CauseAction causeAction = new CauseAction(new CauseStub("Cause1"));
        when(build.getAction(CauseAction.class)).thenReturn(causeAction);

        assertEquals("Cause1", causeContent.getContent(build, null, null, null));
    }

    @Test
    public void shouldReturnMultipleCausesSeperatedByCommas() {
        CauseAction causeAction = mock(CauseAction.class);
        when(causeAction.getCauses()).thenReturn(new LinkedList<Cause>() {{
            add(new CauseStub("Cause1"));
            add(new CauseStub("Cause2"));
            add(new CauseStub("Cause3"));
        }});
        when(build.getAction(CauseAction.class)).thenReturn(causeAction);

        assertEquals("Cause1, Cause2, Cause3", causeContent.getContent(build, null, null, null));
    }

    private class CauseStub extends Cause {
        private String name;

        private CauseStub(String name) {
            this.name = name;
        }

        @Override
        public String getShortDescription() {
            return name;
        }
    }
}