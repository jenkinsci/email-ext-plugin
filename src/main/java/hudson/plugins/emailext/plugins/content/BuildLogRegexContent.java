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

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.tasks.Mailer;
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
	private static final String SUBST_TEXT_NAME = "substText";
	private static final String SUBST_TEXT_DEFAULT_VALUE = null; /* insert entire line */
	
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
			SUBST_TEXT_NAME);
	}
	
	public String getHelpText() {
		return "Displays lines from the build log that match the regular expression.\n" +
			"<ul\n" +
			"<li><i>" + REGEX_ARG_NAME + "</i> - Lines that match this regular expression " +
			"are included. See also <i>java.util.regex.Pattern</i><br>\n" +
			"Defaults to \"" + REGEX_DEFAULT_VALUE + "\".\n" +

			"<li><i>" + LINES_BEFORE_ARG_NAME + "</i> - The number of lines to include " +
			"before the matching line. Lines that overlap with another " +
			"match or <i>linesAfter</i> are only included once.<br>\n" +
			"Defaults to " + LINES_BEFORE_DEFAULT_VALUE + ".\n" +

			"<li><i>" + LINES_AFTER_ARG_NAME + "</i> - The number of lines to include " +
			"after the matching line. Lines that overlap with another " +
			"match or <i>linesBefore</i> are only included once.<br>\n" +
			"Defaults to " + LINES_AFTER_DEFAULT_VALUE + ".\n" +

			"<li><i>" + MAX_MATCHES_ARG_NAME + "</i> - The maximum number of matches " +
			"to include. If 0, all matches will be included.<br>\n" +
			"Defaults to " + MAX_MATCHES_DEFAULT_VALUE + ".\n" +

			"<li><i>" + SHOW_TRUNCATED_LINES_ARG_NAME + "</i> - If <i>true</i>, include " +
			"<tt>[...truncated ### lines...]</tt> lines.<br>\n" +
			"Defaults to " + SHOW_TRUNCATED_LINES_DEFAULT_VALUE + ".\n" +

			"<li><i>" + SUBST_TEXT_NAME + "</i> - If present, insert this text into the email " +
			"rather than the entire line.<br>\n" +

			"</ul>\n";
	}

	private void append(StringBuffer buffer, String line) {
		buffer.append(line);
		buffer.append('\n');
	}
	
	private void appendLinesTruncated(StringBuffer buffer, int numLinesTruncated) {
		// This format comes from hudson.model.Run.getLog(maxLines).
		append(buffer, "[...truncated " + numLinesTruncated + " lines...]");
	}

	public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
	String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
			EmailType emailType, Map<String, ?> args) {
		//LOGGER.log(Level.INFO, TOKEN + " getContent");
		final String regex = Args.get(args, REGEX_ARG_NAME, REGEX_DEFAULT_VALUE);
		final int contextLinesBefore = Args.get(args, LINES_BEFORE_ARG_NAME, LINES_BEFORE_DEFAULT_VALUE);
		final int contextLinesAfter = Args.get(args, LINES_AFTER_ARG_NAME, LINES_AFTER_DEFAULT_VALUE);
		final int maxMatches = Args.get(args, MAX_MATCHES_ARG_NAME, MAX_MATCHES_DEFAULT_VALUE);
		final boolean showTruncatedLines = Args.get(args, SHOW_TRUNCATED_LINES_ARG_NAME, SHOW_TRUNCATED_LINES_DEFAULT_VALUE);
		final String substText = Args.get(args,SUBST_TEXT_NAME, SUBST_TEXT_DEFAULT_VALUE);
		final Pattern pattern = Pattern.compile(regex);
		final StringBuffer buffer = new StringBuffer();
		try {
			final BufferedReader reader = new BufferedReader(new FileReader(build.getLogFile()));
			try {
				int numLinesTruncated = 0;
				int numMatches = 0;
				int numLinesStillNeeded = 0;
				Queue<String> linesBefore = new LinkedList<String>();
				String line = null;
				while ((line = reader.readLine()) != null) {
					// Remove any lines before that are no longer needed.
					while (linesBefore.size() > contextLinesBefore) {
						linesBefore.remove();
						++numLinesTruncated;
					}
					final Matcher matcher = pattern.matcher(line);
					final StringBuffer sb = new StringBuffer();
					boolean bMatched = false;
					while (matcher.find()) {
						bMatched = true;
						if (substText != null)
							matcher.appendReplacement(sb, substText);
						else
							break;
					}
					if (bMatched) {
						// The current line matches.
						if (substText != null)
							matcher.appendTail(sb);
						if (showTruncatedLines == true && numLinesTruncated > 0) {
							// Append information about truncated lines.
							appendLinesTruncated(buffer, numLinesTruncated);
							numLinesTruncated = 0;
						}
						// Append all the linesBefore.
						while (linesBefore.size() > 0) {
							append(buffer, linesBefore.remove());
						}
						// Append the (possibly transformed) current line.
						if (substText != null)
							append(buffer, sb.toString());
						else
							append(buffer, line);
						++numMatches;
						// Set up to add numLinesStillNeeded
						numLinesStillNeeded = contextLinesAfter;
					} else {
						// The current line did not match.
						if (numLinesStillNeeded > 0) {
							// Append this line as a line after.
							append(buffer, line);
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
						appendLinesTruncated(buffer, numLinesTruncated);
					}
				}
			} finally {
				reader.close();
			}
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		
		//LOGGER.log(Level.INFO, "${BUILD_LOG_REGEX,...}:\n" + buffer.toString());
		return buffer.toString();
	}

	public boolean hasNestedContent() {
		return false;
	}
}
