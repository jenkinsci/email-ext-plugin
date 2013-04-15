package hudson.plugins.emailext.plugins;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for CssInliner
 * @author <a href="https://github.com/rahulsom">Rahul Somasunderam</a>
 */
public class CssInlinerTest {
  @Test
  public void testEmailWithoutCss() {
    String input = "<html>" +
        "  <head></head>" +
        "  <body>" +
        "    <span style='color: red;'>Red text</span>" +
        "  </body>" +
        "</html>";
    String output = process(input);
    assertEquals("<html><head></head><body><span style=\"color: red;\">Red text</span></body></html>", output);
  }

  @Test
  public void testEmailWithNormalCss() {
    String input = "<html>" +
        "  <head>" +
        "    <style>" +
        "      span {font-size: 10px;}" +
        "    </style>" +
        "  </head>" +
        "  <body>" +
        "    <span style='color: red;'>Red text</span>" +
        "  </body>" +
        "</html>";
    String output = process(input);
    assertEquals("<html><head><style> span {font-size: 10px;} </style></head>" +
        "<body><span style=\"color: red;\">Red text</span></body></html>", output);
  }

  @Test
  public void testEmailWithInlinedCss() {
    String input = "<html>" +
        "  <head>" +
        "    <style data-inline='true'>" +
        "      span {font-size: 10px;}" +
        "    </style>" +
        "  </head>" +
        "  <body>" +
        "    <span style='color: red;'>Red text</span>" +
        "  </body>" +
        "</html>";
    String output = process(input);
    assertEquals("<html><head></head>" +
        "<body><span style=\"font-size: 10px; color: red;\">Red text</span></body>" +
        "</html>", output);
  }

  @Test
  public void testEmailWithMixedCss() {
    String input = "<html>" +
        "  <head>" +
        "    <style data-inline='true'>" +
        "      span {font-size: 10px;}" +
        "    </style>" +
        "    <style>" +
        "      span {font-family: Verdana;}" +
        "    </style>" +
        "  </head>" +
        "  <body>" +
        "    <span style='color: red;'>Red text</span>" +
        "  </body>" +
        "</html>";
    String output = process(input);
    assertEquals("<html>" +
        "<head><style> span {font-family: Verdana;} </style></head>" +
        "<body><span style=\"font-size: 10px; color: red;\">Red text</span></body></html>", output);
  }

  @Test
  public void testImageInliningOff() {
    String input = "<html>" +
        "  <body>" +
        "    <img src='https://updates.jenkins-ci.org/icons/blank.gif' />" +
        "  </body>" +
        "</html>";

    String output = process(input);
    assertEquals("<html><head></head><body>" +
        "<img src=\"https://updates.jenkins-ci.org/icons/blank.gif\" /></body></html>", output);

  }

  @Test
  public void testImageInliningOn() {
    String input = "<html>" +
        "  <body>" +
        "    <img src='https://updates.jenkins-ci.org/icons/blank.gif' data-inline='true'/>" +
        "  </body>" +
        "</html>";

    String output = process(input);
    String unprocessedExpect = "<html><head></head><body><img src=\"data:image/gif;base64," +
        "R0lGODlhFAAWAKEAAP///8z//wAAAAAAACH+TlRoaXMgYXJ0IGlzIGluIHRoZSBwdWJsaWMgZG9t\n" +
        "YWluLiBLZXZpbiBIdWdoZXMsIGtldmluaEBlaXQuY29tLCBTZXB0ZW1iZXIgMTk5NQAh+QQBAAAB\n" +
        "ACwAAAAAFAAWAAACE4yPqcvtD6OctNqLs968+w+GSQEAOw==\n" +
        "\" data-inline=\"true\" /></body></html>";
    assertEquals(unprocessedExpect.replaceAll("[\r\n]", ""), output.replaceAll("[\r\n]", ""));

  }

  private String process(String input) {
    return new CssInliner().process(input).replaceAll(" +", " ").replaceAll("\n", "").replaceAll("> *<", "><");
  }
}
