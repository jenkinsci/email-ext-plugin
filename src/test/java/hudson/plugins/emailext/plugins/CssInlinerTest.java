package hudson.plugins.emailext.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
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
    @Issue("SECURITY-3705")
    void testImgDataInlineNotFetched() throws Exception {
        AtomicBoolean contacted = new AtomicBoolean(false);
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        server.createContext("/", exchange -> {
            contacted.set(true);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = new URI(
                            "http",
                            null,
                            loopback.getHostAddress(),
                            server.getAddress().getPort(),
                            "/pixel.gif",
                            null,
                            null)
                    .toASCIIString();
            String input = "<html>"
                    + "  <head>"
                    + "    <style data-inline='true'>"
                    + "      span {font-size: 10px;}"
                    + "    </style>"
                    + "  </head>"
                    + "  <body>"
                    + "    <img src='" + url + "' data-inline='true' />"
                    + "  </body>"
                    + "</html>";
            String output = new CssInliner().process(input);
            assertFalse(output.contains("data:"), "image must not be fetched and inlined as a data: URI");
            assertFalse(output.contains("base64"), "image must not be base64-encoded into the body");
        } finally {
            server.stop(0);
        }
        assertFalse(contacted.get(), "CssInliner must not contact the URL referenced by <img data-inline='true'>");
    }

    @Test
    void testNoPrettify() {
        String input = """
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
