package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

public class EmailExtTemplateActionTest {

    @Test
    void testRenderErrorEscapesXSS() throws Exception {
        Exception ex = new RuntimeException("<script>alert('xss')</script>");

        EmailExtTemplateAction action = new EmailExtTemplateAction(null);

        Method method = EmailExtTemplateAction.class.getDeclaredMethod("renderError", Exception.class);
        method.setAccessible(true);

        String result = (String) method.invoke(action, ex);

        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("&lt;script&gt;"));
    }

    @Test
    void testRenderErrorWithNewlines() throws Exception {
        Exception ex = new RuntimeException("line1\nline2");

        EmailExtTemplateAction action = new EmailExtTemplateAction(null);

        Method method = EmailExtTemplateAction.class.getDeclaredMethod("renderError", Exception.class);
        method.setAccessible(true);

        String result = (String) method.invoke(action, ex);

        assertTrue(result.contains("<br/>"));
    }
}
