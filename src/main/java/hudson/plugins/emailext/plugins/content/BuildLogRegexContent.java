/*
 * The MIT License
 * 
 * Copyright (c) 2010, dvrzalik, Stellar Science Ltd Co, K. R. Walker
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
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An EmailContent for build log lines matching a regular expression.
 * Shows lines matching a regular expression (with optional context lines)
 * from the build log file.
 *
 * @author krwalker@stellarscience.com
 */
public class BuildLogRegexContent implements EmailContent {

    private static final Logger LOGGER = Logger.getLogger(Mailer.class.getName());

    private static final String TOKEN = "BUILD_LOG_REGEX";

    private static final String REGEX_ARG_NAME = "regex";

    private static final String REGEX_DEFAULT_VALUE = "(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b";

    private static final String LINES_BEFORE_ARG_NAME = "linesBefore";

    private static final int LINES_BEFORE_DEFAULT_VALUE = 0;

    private static final String LINES_AFTER_ARG_NAME = "linesAfter";

    private static final int LINES_AFTER_DEFAULT_VALUE = 0;

    private static final String MAX_MATCHES_ARG_NAME = "maxMatches";

    private static final int MAX_MATCHES_DEFAULT_VALUE = 0;

    private static final String SHOW_TRUNCATED_LINES_ARG_NAME = "showTruncatedLines";

    private static final boolean SHOW_TRUNCATED_LINES_DEFAULT_VALUE = true;

    private static final String SUBST_TEXT_ARG_NAME = "substText";

    private static final String SUBST_TEXT_DEFAULT_VALUE = null; // insert entire line

    private static final String ESCAPE_HTML_ARG_NAME = "escapeHtml";

    private static final boolean ESCAPE_HTML_DEFAULT_VALUE = false;

    private static final String MATCHED_LINE_HTML_STYLE_ARG_NAME = "matchedLineHtmlStyle";

    private static final String MATCHED_LINE_HTML_STYLE_DEFAULT_VALUE = null;

    private static final String ADD_NEWLINE_ARG_NAME = "addNewline";

    private static final boolean ADD_NEWLINE_DEFAULT_VALUE = true;

    public String getToken() {
        return TOKEN;
    }

    public List<String> getArguments() {
        return Arrays.asList(
                REGEX_ARG_NAME,
                LINES_BEFORE_ARG_NAME,
                LINES_AFTER_ARG_NAME,
                MAX_MATCHES_ARG_NAME,
                SHOW_TRUNCATED_LINES_ARG_NAME,
                SUBST_TEXT_ARG_NAME,
                ESCAPE_HTML_ARG_NAME,
                MATCHED_LINE_HTML_STYLE_ARG_NAME,
                ADD_NEWLINE_ARG_NAME);
    }

    public String getHelpText() {
        return "Displays lines from the build log that match the regular expression.\n"
                + "<ul>\n"
                + "<li><i>" + REGEX_ARG_NAME + "</i> - Lines that match this regular expression "
                + "are included. See also <i>java.util.regex.Pattern</i><br>\n"
                + "Defaults to \"" + REGEX_DEFAULT_VALUE + "\"</li>.\n"
                + "<li><i>" + LINES_BEFORE_ARG_NAME + "</i> - The number of lines to include "
                + "before the matching line. Lines that overlap with another "
                + "match or <i>linesAfter</i> are only included once.<br>\n"
                + "Defaults to " + LINES_BEFORE_DEFAULT_VALUE + ".</li>\n"
                + "<li><i>" + LINES_AFTER_ARG_NAME + "</i> - The number of lines to include "
                + "after the matching line. Lines that overlap with another "
                + "match or <i>linesBefore</i> are only included once.<br>\n"
                + "Defaults to " + LINES_AFTER_DEFAULT_VALUE + ".</li>\n"
                + "<li><i>" + MAX_MATCHES_ARG_NAME + "</i> - The maximum number of matches "
                + "to include. If 0, all matches will be included.<br>\n"
                + "Defaults to " + MAX_MATCHES_DEFAULT_VALUE + ".</li>\n"
                + "<li><i>" + SHOW_TRUNCATED_LINES_ARG_NAME + "</i> - If <i>true</i>, include "
                + "<tt>[...truncated ### lines...]</tt> lines.<br>\n"
                + "Defaults to " + SHOW_TRUNCATED_LINES_DEFAULT_VALUE + ".</li>\n"
                + "<li><i>" + SUBST_TEXT_ARG_NAME + "</i> - If non-null, insert this text into the email "
                + "rather than the entire line.<br>\n"
                + "Defaults to null.</li>\n"
                + "<li><i>" + ESCAPE_HTML_ARG_NAME + "</i> - If true, escape HTML.<br>\n"
                + "Defaults to " + ESCAPE_HTML_DEFAULT_VALUE + ".</li>\n"
                + "<li><i>" + MATCHED_LINE_HTML_STYLE_ARG_NAME + "</i> - If non-null, output HTML. "
                + "matched lines will become <code>&lt;b style=\"your-style-value\"&gt;"
                + "html escaped matched line&lt;/b&gt;</code>.<br>\n"
                + "Defaults to null.</li>\n"
                + "<li><i>" + ADD_NEWLINE_ARG_NAME + "</i> - If true, adds a newline after "
                + "subsText.<br>\n"
                + "Defaults to true.</li>\n"
                + "</ul>\n";
    }

    private boolean startPre(StringBuffer buffer, boolean insidePre) {
        if (!insidePre) {
            buffer.append("<pre>\n");
            insidePre = true;
        }
        return insidePre;
    }

    private boolean stopPre(StringBuffer buffer, boolean insidePre) {
        if (insidePre) {
            buffer.append("</pre>\n");
            insidePre = false;
        }
        return insidePre;
    }

    private void appendContextLine(StringBuffer buffer, String line, boolean escapeHtml) {
        if (escapeHtml) {
            line = StringEscapeUtils.escapeHtml(line);
        }
        buffer.append(line);
        buffer.append('\n');
    }

    private void appendMatchedLine(StringBuffer buffer, String line, boolean escapeHtml, String style, boolean addNewline) {
        if (escapeHtml) {
            line = StringEscapeUtils.escapeHtml(line);
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
        buffer.append(line);
        if (style != null) {
            buffer.append("</b>");
        }

        if(addNewline) {
            buffer.append('\n');
        }
    }

    private void appendLinesTruncated(StringBuffer buffer, int numLinesTruncated, boolean asHtml) {
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
            String transformedContent = getContent(reader, args);
            reader.close();
            return transformedContent;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ""; // TODO: Indicate there was an error instead?
        }
    }

    String getContent(BufferedReader reader, Map<String, ?> args)
            throws IOException {

        final String regex = Args.get(args,
                REGEX_ARG_NAME,
                REGEX_DEFAULT_VALUE);
        final int contextLinesBefore = Args.get(args,
                LINES_BEFORE_ARG_NAME,
                LINES_BEFORE_DEFAULT_VALUE);
        final int contextLinesAfter = Args.get(args,
                LINES_AFTER_ARG_NAME,
                LINES_AFTER_DEFAULT_VALUE);
        final int maxMatches = Args.get(args,
                MAX_MATCHES_ARG_NAME,
                MAX_MATCHES_DEFAULT_VALUE);
        final boolean showTruncatedLines = Args.get(args,
                SHOW_TRUNCATED_LINES_ARG_NAME,
                SHOW_TRUNCATED_LINES_DEFAULT_VALUE);
        final String substText = Args.get(args, SUBST_TEXT_ARG_NAME,
                SUBST_TEXT_DEFAULT_VALUE);
        final String matchedLineHtmlStyle = Args.get(args,
                MATCHED_LINE_HTML_STYLE_ARG_NAME,
                MATCHED_LINE_HTML_STYLE_DEFAULT_VALUE);
        final boolean asHtml = matchedLineHtmlStyle != null;
        final boolean escapeHtml = asHtml
                || Args.get(args,
                ESCAPE_HTML_ARG_NAME,
                ESCAPE_HTML_DEFAULT_VALUE);
        final boolean addNewline = Args.get(args, 
                ADD_NEWLINE_ARG_NAME, 
                ADD_NEWLINE_DEFAULT_VALUE);

        final Pattern pattern = Pattern.compile(regex);
        final StringBuffer buffer = new StringBuffer();
        int numLinesTruncated = 0;
        int numMatches = 0;
        int numLinesStillNeeded = 0;
        boolean insidePre = false;
        Queue<String> linesBefore = new LinkedList<String>();
        String line = null;
        while ((line = reader.readLine()) != null) {
            // Remove console notes (JENKINS-7402)
            line = ConsoleNote.removeNotes(line);

            // Remove any lines before that are no longer needed.
            while (linesBefore.size() > contextLinesBefore) {
                linesBefore.remove();
                ++numLinesTruncated;
            }
            final Matcher matcher = pattern.matcher(line);
            final StringBuffer sb = new StringBuffer();
            boolean matched = false;
            while (matcher.find()) {
                matched = true;
                if (substText != null) {
                    matcher.appendReplacement(sb, substText);
                } else {
                    break;
                }
            }
            if (matched) {
                // The current line matches.
                if (showTruncatedLines == true && numLinesTruncated > 0) {
                    // Append information about truncated lines.
                    insidePre = stopPre(buffer, insidePre);
                    appendLinesTruncated(buffer, numLinesTruncated, asHtml);
                    numLinesTruncated = 0;
                }
                if (asHtml) {
                    insidePre = startPre(buffer, insidePre);
                }
                while (!linesBefore.isEmpty()) {
                    appendContextLine(buffer, linesBefore.remove(), escapeHtml);
                }
                // Append the (possibly transformed) current line.
                if (substText != null) {
                    matcher.appendTail(sb);
                    line = sb.toString();
                }
                appendMatchedLine(buffer, line, escapeHtml, matchedLineHtmlStyle, addNewline);
                ++numMatches;
                // Set up to add numLinesStillNeeded
                numLinesStillNeeded = contextLinesAfter;
            } else {
                // The current line did not match.
                if (numLinesStillNeeded > 0) {
                    // Append this line as a line after.
                    appendContextLine(buffer, line, escapeHtml);
                    --numLinesStillNeeded;
                } else {
                    // Store this line as a possible line before.
                    linesBefore.offer(line);
                }
            }
            if (maxMatches != 0 && numMatches >= maxMatches && numLinesStillNeeded == 0) {
                break;
            }
        }
        if (showTruncatedLines == true) {
            // Count the rest of the lines.
            // Include any lines in linesBefore.
            while (linesBefore.size() > 0) {
                linesBefore.remove();
                ++numLinesTruncated;
            }
            if (line != null) {
                // Include the rest of the lines that haven't been read in.
                while ((line = reader.readLine()) != null) {
                    ++numLinesTruncated;
                }
            }
            if (numLinesTruncated > 0) {
                insidePre = stopPre(buffer, insidePre);
                appendLinesTruncated(buffer, numLinesTruncated, asHtml);
            }
        }
        insidePre = stopPre(buffer, insidePre);
        return buffer.toString();
    }

    public boolean hasNestedContent() {
        return false;
    }
}
