package hudson.plugins.emailext.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Tests for CssInliner
 *
 * @author <a href="https://github.com/rahulsom">Rahul Somasunderam</a>
 */
public class CssInlinerTest {

  @Test
  public void testEmailWithoutCss() {
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
    public void testEntities() {
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
        assertEquals("<html><head></head><body><h1>Compte rendu d'installation sur WMS11DEV</h1>"
                + "<p> Veuillez trouver la liste des patchs installés sur l'environnement WMS11DEV "
                + ": </p></body></html>", output);
  }

  @Test
  public void testEmailWithNormalCss() {
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
  public void testEmailWithInlinedCss() {
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
        assertEquals("<html><head></head>"
                + "<body><span style=\"font-size: 10px; color: red;\">Red text</span></body>"
                + "</html>", output);
  }

  @Test
  public void testEmailWithMixedCss() {
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
        assertEquals("<html>"
                + "<head><style> span {font-family: Verdana;} </style></head>"
                + "<body><span style=\"font-size: 10px; color: red;\">Red text</span></body></html>", output);
  }

  @Test
  public void testImageInliningOff() {
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
  public void testImageInliningOn() {
        String input = "<html>"
                + "  <body>"
                + "    <img src='"
                + getClass().getClassLoader().getResource("blank.gif").toExternalForm()
                + "' data-inline='true' />"
                + "  </body>"
                + "</html>";

        String output = process(input);
        String unprocessedExpect = "<html><head></head><body><img src=\"data:image/gif;base64,"
                + "R0lGODlhFAAWAKEAAP///8z//wAAAAAAACH+TlRoaXMgYXJ0IGlzIGluIHRoZSBwdWJsaWMgZG9t\n"
                + "YWluLiBLZXZpbiBIdWdoZXMsIGtldmluaEBlaXQuY29tLCBTZXB0ZW1iZXIgMTk5NQAh+QQBAAAB\n"
                + "ACwAAAAAFAAWAAACE4yPqcvtD6OctNqLs968+w+GSQEAOw==\n"
                + "\" data-inline=\"true\" /></body></html>";

    assertEquals(unprocessedExpect.replaceAll("[\r\n]", ""), output.replaceAll("[\r\n]", ""));
  }

  @Test
  public void testNoPrettify() {
        String input = "<html><head></head>\n"
                + "<body>\n"
                + "<table border=\"1\">\n"
                + "<tbody>\n"
                + "<tr>\n"
                + "<td> <b>TEXT</b> </td>\n"
                + "<td> <b>TEXT</b><pre>\n"
                + "line\n"
                + "line<v1 />line\n"
                + "line<v1 />line\n"
                + "</pre>\n"
                + "</td>\n"
                + "</tr></tbody></table></body></html>";
      String output = new CssInliner().process(input);
      assertEquals(input, output);
  }

  private String process(String input) {
        return clean(new CssInliner().process(input));
    }

    private String clean(String input) {
        return input.replaceAll(" +", " ").replaceAll("\n", "").replaceAll("> *<", "><");
  }
}
