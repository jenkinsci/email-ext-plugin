/*
 * The MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.emailext.plugins.content;


import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.tasks.Mailer;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An EmailContent for build log segments matching a regular expression. The
 * regular expression will be matched against the whole content of the build log,
 * including line terminators.
 * Shows build log segments matching a regular expression from the build log file.
 *
 * @author krwalker@stellarscience.com
 */
public class BuildLogMultilineRegexContent implements EmailContent {

  private static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

  private static final String TOKEN = "BUILD_LOG_MULTILINE_REGEX";

  private static final String REGEX_ARG_NAME = "regex";

  private static final String MAX_MATCHES_ARG_NAME = "maxMatches";

  private static final int MAX_MATCHES_DEFAULT_VALUE = 0;

  private static final String SHOW_TRUNCATED_LINES_ARG_NAME = "showTruncatedLines";

  private static final boolean SHOW_TRUNCATED_LINES_DEFAULT_VALUE = true;

  private static final String SUBST_TEXT_ARG_NAME = "substText";

  private static final String SUBST_TEXT_DEFAULT_VALUE = null; // insert entire segment

  private static final String ESCAPE_HTML_ARG_NAME = "escapeHtml";

  private static final boolean ESCAPE_HTML_DEFAULT_VALUE = false;

  private static final String MATCHED_SEGMENT_HTML_STYLE_ARG_NAME = "matchedSegmentHtmlStyle";

  private static final String MATCHED_SEGMENT_HTML_STYLE_DEFAULT_VALUE = null;

  private static final Pattern LINE_TERMINATOR_PATTERN = Pattern.compile("(?<=.)\\r?\\n");

  public String getToken() {
    return TOKEN;
  }

  public List<String> getArguments() {
    return Arrays.asList(
      REGEX_ARG_NAME,
      MAX_MATCHES_ARG_NAME,
      SHOW_TRUNCATED_LINES_ARG_NAME,
      SUBST_TEXT_ARG_NAME,
      ESCAPE_HTML_ARG_NAME,
      MATCHED_SEGMENT_HTML_STYLE_ARG_NAME);
  }

  public String getHelpText() {
    return "Displays build log segments that match the regular expression.\n"
           + "<ul>\n"
           + "<li><i>" + REGEX_ARG_NAME
           + "</i> - Segments of the build log that match this regular expression "
           + "are included.  See also <a href=\""
           + "http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html\">"
           + "<i>java.util.regex.Pattern</i></a><br>\n"
           + "No default. Required parameter."
           + "<li><i>" + MAX_MATCHES_ARG_NAME + "</i> - The maximum number of matches "
           + "to include. If 0, all matches will be included.<br>\n"
           + "Defaults to " + MAX_MATCHES_DEFAULT_VALUE + ".\n"
           + "<li><i>" + SHOW_TRUNCATED_LINES_ARG_NAME + "</i> - If <i>true</i>, include "
           + "<tt>[...truncated ### lines...]</tt> lines.<br>\n"
           + "Defaults to " + SHOW_TRUNCATED_LINES_DEFAULT_VALUE + ".\n"
           + "<li><i>" + SUBST_TEXT_ARG_NAME + "</i> - If non-null, insert this text into the email "
           + "rather than the entire segment.<br>\n"
           + "Defaults to null.\n"
           + "<li><i>" + ESCAPE_HTML_ARG_NAME + "</i> - If true, escape HTML.<br>\n"
           + "Defaults to " + ESCAPE_HTML_DEFAULT_VALUE + ".\n"
           + "<li><i>" + MATCHED_SEGMENT_HTML_STYLE_ARG_NAME + "</i> - If non-null, output HTML. "
           + "matched lines will become <code>&lt;b style=\"your-style-value\"&gt;"
           + "html escaped matched line&lt;/b&gt;</code>.<br>\n"
           + "Defaults to null.\n"
           + "</ul>\n";
  }

  private boolean startPre(StringBuilder buffer, boolean insidePre) {
    if (!insidePre) {
      buffer.append("<pre>\n");
      insidePre = true;
    }
    return insidePre;
  }

  private boolean stopPre(StringBuilder buffer, boolean insidePre) {
    if (insidePre) {
      buffer.append("</pre>\n");
      insidePre = false;
    }
    return insidePre;
  }

  private void appendMatchedSegment(StringBuilder buffer, String segment,
                                    boolean escapeHtml, String style) {
    if (escapeHtml) {
      segment = StringEscapeUtils.escapeHtml(segment);
    }
    if (style != null) {
      buffer.append("<b");
      if ( style.length() > 0 ) {
        buffer.append(" style=\"");
        buffer.append(style);
        buffer.append("\"");
      }
      buffer.append(">");
    }
    buffer.append(segment);
    if (style != null) {
      buffer.append("</b>");
    }
    buffer.append('\n');
  }

  private void appendLinesTruncated(StringBuilder buffer, int numLinesTruncated, boolean asHtml) {
    // This format comes from hudson.model.Run.getLog(maxLines).
    if (asHtml) {
      buffer.append("<p>");
    }
    buffer.append("[...truncated ");
    buffer.append(numLinesTruncated);
    buffer.append(" lines...]");
    if (asHtml) {
      buffer.append("</p>");
    }
    buffer.append('\n');
  }

  public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(
    AbstractBuild<P, B> build, ExtendedEmailPublisher publisher, EmailType emailType, Map<String, ?> args) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(build.getLogFile()));
      try {
        return getContent(reader, args);
      } finally {
        reader.close();
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return ""; // TODO: Indicate there was an error instead?
    }
  }

  String getContent(BufferedReader reader, Map<String,?> args) throws IOException {
    final Pattern pattern = Pattern.compile((String)args.get(REGEX_ARG_NAME));
    final int maxMatches
      = Args.get(args, MAX_MATCHES_ARG_NAME, MAX_MATCHES_DEFAULT_VALUE);
    final boolean showTruncatedLines
      = Args.get(args, SHOW_TRUNCATED_LINES_ARG_NAME,
                 SHOW_TRUNCATED_LINES_DEFAULT_VALUE);
    final String substText
      = Args.get(args, SUBST_TEXT_ARG_NAME, SUBST_TEXT_DEFAULT_VALUE);
    final String matchedSegmentHtmlStyle
      = Args.get(args, MATCHED_SEGMENT_HTML_STYLE_ARG_NAME,
                 MATCHED_SEGMENT_HTML_STYLE_DEFAULT_VALUE);
    final boolean asHtml = matchedSegmentHtmlStyle != null;
    final boolean escapeHtml
      = asHtml || Args.get(args, ESCAPE_HTML_ARG_NAME, ESCAPE_HTML_DEFAULT_VALUE);

    StringBuilder line = new StringBuilder();
    StringBuilder fullLog = new StringBuilder();
    int ch;
    // Buffer log contents including line terminators, and remove console notes
    while ((ch = reader.read()) != -1) {
      if (ch == '\r' || ch == '\n') {
        if (line.length() > 0) {
          // Remove console notes (JENKINS-7402)
          fullLog.append(ConsoleNote.removeNotes(line.toString()));
          line.setLength(0);
        }
        fullLog.append((char)ch);
      } else {
        line.append((char)ch);
      }
    }
    // Buffer the final log line if it has no line terminator
    if (line.length() > 0) {
      // Remove console notes (JENKINS-7402)
      fullLog.append(ConsoleNote.removeNotes(line.toString()));
    }
    StringBuilder content = new StringBuilder();
    int numMatches = 0;
    boolean insidePre = false;
    int lastMatchEnd = 0;
    final Matcher matcher = pattern.matcher(fullLog);
    while (matcher.find()) {
      if (maxMatches != 0 && ++numMatches > maxMatches) {
        break;
      }
      if (showTruncatedLines) {
        if (matcher.start() > lastMatchEnd) {
          // Append information about truncated lines.
          int numLinesTruncated = countLineTerminators
              (fullLog.subSequence(lastMatchEnd, matcher.start()));
          if (numLinesTruncated > 0) {
            insidePre = stopPre(content, insidePre);
            appendLinesTruncated(content, numLinesTruncated, asHtml);
          }
        }
      }
      if (asHtml) {
        insidePre = startPre(content, insidePre);
      }
      if (substText != null) {
        final StringBuffer substBuf = new StringBuffer();
        matcher.appendReplacement(substBuf, substText);
        // Remove prepended text between matches
        final String segment = substBuf.substring(matcher.start() - lastMatchEnd);
        appendMatchedSegment
            (content, segment, escapeHtml, matchedSegmentHtmlStyle);
      } else {
        appendMatchedSegment
            (content, matcher.group(), escapeHtml, matchedSegmentHtmlStyle);
      }
      lastMatchEnd = matcher.end();
    }
    if (showTruncatedLines) {
      if (fullLog.length() > lastMatchEnd) {
        // Append information about truncated lines.
        int numLinesTruncated = countLineTerminators
            (fullLog.subSequence(lastMatchEnd, fullLog.length()));
        if (numLinesTruncated > 0) {
          insidePre = stopPre(content, insidePre);
          appendLinesTruncated(content, numLinesTruncated, asHtml);
        }
      }
    }
    stopPre(content, insidePre);
    return content.toString();
  }

  private int countLineTerminators(CharSequence charSequence) {
    int lineTerminatorCount = 0;
    Matcher matcher = LINE_TERMINATOR_PATTERN.matcher(charSequence);
    while (matcher.find()) {
      ++lineTerminatorCount;
    }
    return lineTerminatorCount;
  }

  public boolean hasNestedContent() {
    return false;
  }
}
