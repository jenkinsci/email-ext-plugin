package hudson.plugins.emailext.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Tests for CssInliner
 *
 * @author <a href="https://github.com/rahulsom">Rahul Somasunderam</a>
 */
class CssInlinerTest {

    @Test
    void testEmailWithoutCss() {
        String input = "<html>"
                + "  <head></head>"
                + "  <body>"
                + "    <span style='color: red;'>Red text</span>"
                + "  </body>"
                + "</html>";
        String output = process(input);
        assertEquals(clean(input), output);
    }

    @Test
    @Issue("JENKINS-25719")
    void testEntities() {
        String input = "<html>"
                + "  <head>"
                + "    <style data-inline='true'>"
                + "      span {font-size: 10px;}"
                + "    </style>"
                + "  </head>"
                + "  <body>"
                + "    <h1>Compte rendu d'installation sur WMS11DEV</h1>"
                + "    <p>"
                + "    Veuillez trouver la liste des patchs installés sur l'environnement WMS11DEV :"
                + "    </p>"
                + "  </body>"
                + "</html>";
        String output = process(input);
        assertEquals(
                "<html><head></head><body><h1>Compte rendu d'installation sur WMS11DEV</h1>"
                        + "<p> Veuillez trouver la liste des patchs installés sur l'environnement WMS11DEV "
                        + ": </p></body></html>",
                output);
    }

    @Test
    void testEmailWithNormalCss() {
        String input = "<html>"
                + "  <head>"
                + "    <style>"
                + "      span {font-size: 10px;}"
                + "    </style>"
                + "  </head>"
                + "  <body>"
                + "    <span style='color: red;'>Red text</span>"
                + "  </body>"
                + "</html>";
        String output = process(input);
        assertEquals(clean(input), output);
    }

    @Test
    void testEmailWithInlinedCss() {
        String input = "<html>"
                + "  <head>"
                + "    <style data-inline='true'>"
                + "      span {font-size: 10px;}"
                + "    </style>"
                + "  </head>"
                + "  <body>"
                + "    <span style='color: red;'>Red text</span>"
                + "  </body>"
                + "</html>";
        String output = process(input);
        assertEquals(
                "<html><head></head>"
                        + "<body><span style=\"font-size: 10px; color: red;\">Red text</span></body>"
                        + "</html>",
                output);
    }

    @Test
    void testEmailWithMixedCss() {
        String input = "<html>"
                + "  <head>"
                + "    <style data-inline='true'>"
                + "      span {font-size: 10px;}"
                + "    </style>"
                + "    <style>"
                + "      span {font-family: Verdana;}"
                + "    </style>"
                + "  </head>"
                + "  <body>"
                + "    <span style='color: red;'>Red text</span>"
                + "  </body>"
                + "</html>";
        String output = process(input);
        assertEquals(
                "<html>"
                        + "<head><style>/*<![CDATA[*/ span {font-family: Verdana;} /*]]>*/</style></head>"
                        + "<body><span style=\"font-size: 10px; color: red;\">Red text</span></body></html>",
                output);
    }

    @Test
    void testImageInliningOff() {
        String input = "<html>"
                + "  <body>"
                + "    <img src='"
                + getClass().getClassLoader().getResource("blank.gif").toExternalForm()
                + "' />"
                + "  </body>"
                + "</html>";

        String output = process(input);
        assertEquals(clean(input), output);
    }

    @Test
    void testImageInliningOn() {
        String input = "<html>"
                + "  <body>"
                + "    <img src='"
                + getClass().getClassLoader().getResource("blank.gif").toExternalForm()
                + "' data-inline='true' />"
                + "  </body>"
                + "</html>";

        String output = process(input);
        String unprocessedExpect =
                """
                <html><head></head><body><img src="data:image/gif;base64,\
                R0lGODlhFAAWAKEAAP///8z//wAAAAAAACH+TlRoaXMgYXJ0IGlzIGluIHRoZSBwdWJsaWMgZG9t
                YWluLiBLZXZpbiBIdWdoZXMsIGtldmluaEBlaXQuY29tLCBTZXB0ZW1iZXIgMTk5NQAh+QQBAAAB
                ACwAAAAAFAAWAAACE4yPqcvtD6OctNqLs968+w+GSQEAOw==
                " data-inline="true" /></body></html>""";

        assertEquals(unprocessedExpect.replaceAll("[\r\n]", ""), output.replaceAll("[\r\n]", ""));
    }

    @Test
    void testNoPrettify() {
        String input =
                """
                <html><head></head>
                <body>
                <table border="1">
                <tbody>
                <tr>
                <td> <b>TEXT</b> </td>
                <td> <b>TEXT</b><pre>
                line
                line<v1 />line
                line<v1 />line
                </pre>
                </td>
                </tr></tbody></table></body></html>""";
        String output = new CssInliner().process(input);
        assertEquals(input, output);
    }

    private static String process(String input) {
        return clean(new CssInliner().process(input));
    }

    private static String clean(String input) {
        return input.replaceAll(" +", " ").replaceAll("\n", "").replaceAll("> *<", "><");
    }
}
