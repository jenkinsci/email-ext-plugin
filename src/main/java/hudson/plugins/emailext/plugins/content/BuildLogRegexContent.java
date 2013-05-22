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
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * An EmailContent for build log lines matching a regular expression. Shows
 * lines matching a regular expression (with optional context lines) from the
 * build log file.
 *
 * @author krwalker@stellarscience.com
 */
@EmailToken
public class BuildLogRegexContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "BUILD_LOG_REGEX";
    private static final int LINES_BEFORE_DEFAULT_VALUE = 0;
    private static final int LINES_AFTER_DEFAULT_VALUE = 0;
    private static final int MAX_MATCHES_DEFAULT_VALUE = 0;
    @Parameter
    public String regex = "(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b";
    @Parameter
    public int linesBefore = LINES_BEFORE_DEFAULT_VALUE;
    @Parameter
    public int linesAfter = LINES_AFTER_DEFAULT_VALUE;
    @Parameter
    public int maxMatches = MAX_MATCHES_DEFAULT_VALUE;
    @Parameter
    public boolean showTruncatedLines = true;
    @Parameter
    public String substText = null; // insert entire line
    @Parameter
    public boolean escapeHtml = false;
    @Parameter
    public String matchedLineHtmlStyle = null;
    @Parameter
    public boolean addNewline = true;
    @Parameter
    public String defaultValue = "";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
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
            if (style.length() > 0) {
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

        if (addNewline) {
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

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        try {
            BufferedReader reader = new BufferedReader(build.getLogReader());
            String transformedContent = getContent(reader);
            reader.close();
            return transformedContent;
        } catch (IOException ex) {
            listener.error(ex.getMessage());
            return ""; // TODO: Indicate there was an error instead?
        }
    }

    String getContent(BufferedReader reader)
            throws IOException {

        final boolean asHtml = matchedLineHtmlStyle != null;
        escapeHtml = asHtml || escapeHtml;

        final Pattern pattern = Pattern.compile(regex);
        final StringBuffer buffer = new StringBuffer();
        int numLinesTruncated = 0;
        int numMatches = 0;
        int numLinesStillNeeded = 0;
        boolean insidePre = false;
        Queue<String> linesBeforeList = new LinkedList<String>();
        String line = null;
        while ((line = reader.readLine()) != null) {
            // Remove console notes (JENKINS-7402)
            line = ConsoleNote.removeNotes(line);

            // Remove any lines before that are no longer needed.
            while (linesBeforeList.size() > linesBefore) {
                linesBeforeList.remove();
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
                while (!linesBeforeList.isEmpty()) {
                    appendContextLine(buffer, linesBeforeList.remove(), escapeHtml);
                }
                // Append the (possibly transformed) current line.
                if (substText != null) {
                    matcher.appendTail(sb);
                    line = sb.toString();
                }
                appendMatchedLine(buffer, line, escapeHtml, matchedLineHtmlStyle, addNewline);
                ++numMatches;
                // Set up to add numLinesStillNeeded
                numLinesStillNeeded = linesAfter;
            } else {
                // The current line did not match.
                if (numLinesStillNeeded > 0) {
                    // Append this line as a line after.
                    appendContextLine(buffer, line, escapeHtml);
                    --numLinesStillNeeded;
                } else {
                    // Store this line as a possible line before.
                    linesBeforeList.offer(line);
                }
            }
            if (maxMatches != 0 && numMatches >= maxMatches && numLinesStillNeeded == 0) {
                break;
            }
        }
        if (showTruncatedLines == true) {
            // Count the rest of the lines.
            // Include any lines in linesBefore.
            while (linesBeforeList.size() > 0) {
                linesBeforeList.remove();
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
        if (buffer.length() == 0) {
            return defaultValue;
        }
        return buffer.toString();
    }
}
