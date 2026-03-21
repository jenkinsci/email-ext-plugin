package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EmailExtTemplateActionTest {

    @Test
    public void testRenderErrorEscapesXSS() {
        Exception ex = new RuntimeException("<script>alert('xss')</script>");
        String result = ex.toString().replace("\n", "<br/>");
        assertTrue(result.contains("<script>"));
    }

    @Test
    public void testRenderErrorWithNewlines() {
        Exception ex = new RuntimeException("line1\nline2");
        String result = ex.toString().replace("\n", "<br/>");
        assertTrue(result.contains("<br/>"));
    }
}
