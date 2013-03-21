package hudson.plugins.emailext.plugins;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.StringTokenizer;

/**
 * Inlines CSS to avoid the dreaded GMail Grimace.
 * @author: <a href="https://github.com/rahulsom">Rahul Somasunderam</a>
 */
public class CssInliner {
  boolean debug = true;

  private static String concatenateProperties(String oldProp, String newProp) {
    if (!oldProp.endsWith(";"))
      oldProp += ";";
    return oldProp.trim() +" " + newProp.trim() + ";";
  }

  private String fetchStyles(Document doc) {
    Elements els = doc.select("style");
    StringBuilder styles = new StringBuilder();
    for (Element e : els) {
      if (e.attr("data-inline").equals("true")) {
        styles.append(e.data());
        e.remove();
      }
    }
    return styles.toString();
  }

  public String process(String input) {

    Document doc = Jsoup.parse(input);
    String stylesheet = fetchStyles(doc);

    String trimmedStylesheet = stylesheet.replaceAll("\n", "").replaceAll("/\\*.*?\\*/", "").replaceAll(" +", " ");
    String styleRules = trimmedStylesheet.trim(), delims = "{}";
    StringTokenizer st = new StringTokenizer(styleRules, delims);
    while (st.countTokens() > 1) {
      String selector = st.nextToken(), properties = st.nextToken();
      Elements selectedElements = doc.select(selector);
      for (Element selElem : selectedElements) {
        String oldProperties = selElem.attr("cssstyle");
        selElem.attr("cssstyle",
            oldProperties.length() > 0 ? concatenateProperties(
                oldProperties, properties) : properties);
      }
    }

    Elements allStyledElements = doc.getElementsByAttribute("cssstyle");

    for (Element e: allStyledElements) {
      String newStyle = e.attr("cssstyle");
      String oldStyle = e.attr("style");
      e.attr("style", (newStyle +"; "+ oldStyle).replace(";;", ";"));
      e.removeAttr("cssstyle");
    }

    String output = doc.toString().replaceAll("&quot;", "'");
    return output;
  }
}
