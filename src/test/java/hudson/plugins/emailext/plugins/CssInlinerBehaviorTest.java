package hudson.plugins.emailext.plugins;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test to investigate CssInliner behavior
 *
 * @author Akash Manna
 */
class CssInlinerBehaviorTest {

    @Test
    void testProcessingWithoutDataInline() {
        CssInliner inliner = new CssInliner();
        String input = "<html><body><p>Hello &amp; Welcome</p><p>Price &lt; $100</p></body></html>";

        String output = inliner.process(input);

        assertEquals(input, output, "Should return input unchanged when no data-inline elements");
    }

    @Test
    void testProcessingWithDataInline() {
        CssInliner inliner = new CssInliner();
        String input = "<html><head><style data-inline=\"true\">p { color: red; }</style></head>"
                + "<body><p>Hello &amp; Welcome</p><p>Price &lt; $100</p></body></html>";

        String output = inliner.process(input);

        System.out.println("Input: " + input);
        System.out.println("Output: " + output);

        assertTrue(output.contains("style="), "Should have inlined styles");

        assertFalse(output.contains("<?xml"), "Should not have XML declaration");

        assertTrue(output.contains("&amp;"), "HTML entity &amp; should remain escaped");
        assertTrue(output.contains("&lt;"), "HTML entity &lt; should remain escaped");

        assertFalse(output.contains("Hello & Welcome"), "Should not contain unescaped &");
        assertFalse(output.contains("Price < $100"), "Should not contain unescaped <");

        System.out.println("Contains '&amp;': " + output.contains("&amp;"));
        System.out.println("Contains '&lt;': " + output.contains("&lt;"));
    }

    @Test
    void testStripHtml() {
        CssInliner inliner = new CssInliner();
        String input = "<html><body><p>Hello <b>World</b></p></body></html>";

        String output = inliner.stripHtml(input);

        System.out.println("HTML input: " + input);
        System.out.println("Stripped output: " + output);

        assertFalse(output.contains("<"), "Should not contain HTML tags");
        assertTrue(output.contains("Hello"), "Should contain text content");
    }
}
