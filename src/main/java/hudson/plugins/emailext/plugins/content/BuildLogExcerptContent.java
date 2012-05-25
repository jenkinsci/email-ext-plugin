/*
 * The MIT License
 *
 * Copyright (c) 2011, CloudBees, Inc., Nicolas De Loof
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
public class BuildLogExcerptContent implements EmailContent {

    private static final Logger LOGGER = Logger.getLogger(BuildLogExcerptContent.class.getName());

    public String getToken() {
        return "BUILD_LOG_EXCERPT";
    }

    public List<String> getArguments() {
        return Arrays.asList(
                "start", "end");
    }

    public String getHelpText() {
        return "Displays an excerpt from the build log.\n"
                + "<ul>\n"
                + "<li><i>start</i> - Regular expression to match the excerpt starting line to be included (exluded). "
                + "See <a href='http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html'><i>java.util.regex.Pattern</i></a></li>"
                + "<li><i>end</i> - Regular expression to match the excerpt ending line to be included (exluded)</li>"
                + "</ul>";
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher, EmailType emailType, Map<String, ?> args) throws IOException, InterruptedException {
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

    String getContent(BufferedReader reader, Map<String, ?> args) throws IOException {

        Pattern start = Pattern.compile((String)args.get("start"));
        Pattern end = Pattern.compile((String)args.get("end"));

        StringBuilder buffer = new StringBuilder();
        String line = null;
        boolean started = false;
        while ((line = reader.readLine()) != null) {
            line = ConsoleNote.removeNotes(line);

            if (start.matcher(line).matches()) {
                started = true;
                continue;
            }
            if (end.matcher(line).matches()) break;

            if (started) buffer.append(line).append('\n');
        }
        return buffer.toString();
    }

    public boolean hasNestedContent() {
        return false;
    }
}
