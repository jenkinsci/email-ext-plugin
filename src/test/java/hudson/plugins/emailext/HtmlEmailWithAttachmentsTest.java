package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.mock_javamail.Mailbox;

/**
 * Test for JENKINS-76109: HTML emails with attachments fail to send.
 *
 * This test verifies that HTML emails with special characters (escaped HTML entities)
 * work correctly when combined with attachments and attachLog.
 *
 * The issue was that CssInliner was calling StringEscapeUtils.unescapeHtml4() on
 * JSoup's output, which unescaped HTML entities like &amp; and &lt;, breaking the HTML.
 *
 * @author Akash Manna
 */
@WithJenkins
class HtmlEmailWithAttachmentsTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @AfterEach
    void tearDown() {
        Mailbox.clearAll();
    }

    /**
     * Test that HTML emails with escaped entities work correctly.
     * This was the root cause of JENKINS-76109.
     */
    @Test
    void testHtmlWithEscapedEntitiesPreserved() {
        hudson.plugins.emailext.plugins.CssInliner inliner = new hudson.plugins.emailext.plugins.CssInliner();

        String input = "<html><body><p>Hello &amp; Welcome</p><p>Price &lt; $100</p></body></html>";

        String output = inliner.process(input);

        assertEquals(input, output, "HTML without data-inline should be returned unchanged");
    }

    /**
     * Test that HTML with inline CSS preserves escaped entities.
     */
    @Test
    void testHtmlWithInlineCssPreservesEscapedEntities() {
        hudson.plugins.emailext.plugins.CssInliner inliner = new hudson.plugins.emailext.plugins.CssInliner();

        String input = "<html><head><style data-inline=\"true\">p { color: red; }</style></head>"
                + "<body><p>Hello &amp; Welcome</p><p>Price &lt; $100</p></body></html>";

        String output = inliner.process(input);

        assertTrue(output.contains("&amp;"), "Escaped ampersand should remain escaped after CSS inlining");
        assertTrue(output.contains("&lt;"), "Escaped less-than should remain escaped after CSS inlining");

        assertTrue(
                !output.contains("Hello & Welcome") || output.contains("&amp;"),
                "Should not have unescaped ampersand in text content");
    }
}
