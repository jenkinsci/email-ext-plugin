package hudson.plugins.emailext.plugins;

import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.htmlparser.jericho.Source;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;

/**
 * <p>
 * Inlines CSS to avoid the dreaded GMail Grimace.</p>
 *
 * <p>
 * The magic keyword is <code><em>data-inline="true"</em></code>.</p>
 *
 * <ul>
 * <li>
 * When used in conjunction with the <code>style</code> tag, it inlines the
 * stylesheet defined there into all html elements matching the rules.
 * </li>
 * <li>
 * When used with the <code>img</code> tag, it base64 encodes the image it found
 * to make it visible in the email.
 * </li>
 * </ul>
 *
 * @author <a href="https://github.com/rahulsom">Rahul Somasunderam</a>
 */
public class CssInliner {
    private static final Logger LOG = Logger.getLogger(CssInliner.class.getName());

    public static final String CSS_STYLE = "cssstyle";
    public static final String STYLE_ATTR = "style";
    public static final String STYLE_TAG = "style";
    public static final String IMG_TAG = "img";
    public static final String IMG_SRC_ATTR = "src";
    public static final String DATA_INLINE_ATTR = "data-inline";

    private static String concatenateProperties(String oldProp, String newProp) {
        if (!oldProp.endsWith(";")) {
            oldProp += ";";
        }
        return oldProp.trim() + " " + newProp.trim() + ";";
    }

    /**
     * Generates a stylesheet from an html document
     *
     * @param doc the html document
     * @return a string representing the stylesheet.
     */
    private String fetchStyles(Document doc) {
        Elements els = doc.select(STYLE_TAG);
        StringBuilder styles = new StringBuilder();
        for (Element e : els) {
            if (e.attr(DATA_INLINE_ATTR).equals("true")) {
                styles.append(e.data());
                e.remove();
            }
        }
        return styles.toString();
    }

    public String stripHtml(String input) {
        return new Source(input).getRenderer().toString();
    }

    /**
     * Takes an input string representing an html document and processes it with
     * the Css Inliner.
     *
     * @param input the html document
     * @return the processed html document
     */
    public String process(String input) {
        Document doc = Jsoup.parse(input);

        // check if the user wants to inline the data
        Elements elements = doc.getElementsByAttributeValue(DATA_INLINE_ATTR, "true");
        if (elements.isEmpty()) {
            return input;
        }

        extractStyles(doc);
        applyStyles(doc);
        inlineImages(doc);

        doc.outputSettings(doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).prettyPrint(false).escapeMode(Entities.EscapeMode.extended));
        return StringEscapeUtils.unescapeHtml(doc.outerHtml());
    }

    /**
     * Inlines images marked with <code>data-inline="true"</code>
     *
     * @param doc the html document
     */
    private void inlineImages(Document doc) {
        Elements allImages = doc.getElementsByTag(IMG_TAG);
        for (Element img : allImages) {
            if (img.attr(DATA_INLINE_ATTR).equals("true")) {
                String src = img.attr(IMG_SRC_ATTR);
                try {
                    URL url = new URL(src);
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.connect();
                    String contentType = urlConnection.getContentType();

                    urlConnection.getContent();
                    byte[] srcContent = IOUtils.toByteArray(url.openStream());
                    String base64 = Base64.getEncoder().encodeToString(srcContent);

                    img.attr(IMG_SRC_ATTR, MessageFormat.format("data:{0};base64,{1}", contentType, base64));
                } catch (Exception e) {
                    LOG.log(Level.WARNING, null, e);
                }
            }
        }
    }

    /**
     * Transfers styles from the <code>cssstyle</code> attribute to the
     * <code>style</code> attribute.
     *
     * @param doc the html document
     */
    private void applyStyles(Document doc) {
        Elements allStyledElements = doc.getElementsByAttribute(CSS_STYLE);

        for (Element e : allStyledElements) {
            String newStyle = e.attr(CSS_STYLE);
            String oldStyle = e.attr(STYLE_ATTR);
            e.attr(STYLE_ATTR, (newStyle + "; " + oldStyle).replace(";;", ";"));
            e.removeAttr(CSS_STYLE);
        }
    }

    /**
     * Extracts styles from the stylesheet and applies them to a
     * <code>cssstyle</code> attribute. This is because the styles need to be
     * applied sequentially, but before the <code>style</code> defined for the
     * element inline.
     *
     * @param doc the html document
     */
    private void extractStyles(Document doc) {
        String stylesheet = fetchStyles(doc);

        String trimmedStylesheet = stylesheet.replaceAll("\n", "").replaceAll("/\\*.*?\\*/", "").replaceAll(" +", " ");
        String styleRules = trimmedStylesheet.trim();
        String delims = "{}";
        StringTokenizer st = new StringTokenizer(styleRules, delims);
        while (st.countTokens() > 1) {
            String selector = st.nextToken().trim();
            String properties = st.nextToken().trim();
            Elements selectedElements = doc.select(selector);
            for (Element selElem : selectedElements) {
                String oldProperties = selElem.attr(CSS_STYLE);
                selElem.attr(CSS_STYLE,
                        oldProperties.length() > 0 ? concatenateProperties(
                                        oldProperties, properties) : properties);
            }
        }
    }
}
