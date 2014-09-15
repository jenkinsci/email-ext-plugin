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

import hudson.FilePath;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
@EmailToken
public class BuildLogExcerptContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "BUILD_LOG_EXCERPT";
    
    @Parameter(required=true)
    public String start;
    
    @Parameter(required=true)
    public String end;
    
    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(Run<?, ?> context, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        try {
            BufferedReader reader = new BufferedReader(context.getLogReader());
            try {
                return getContent(reader);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        } catch (IOException e) {
            listener.getLogger().println("Error getting BUILD_LOG_EXCERPT - " + e.getMessage());
            return ""; // TODO: Indicate there was an error instead?
        }
    }

    String getContent(BufferedReader reader) throws IOException {

        Pattern startPattern = Pattern.compile(start);
        Pattern endPattern = Pattern.compile(end);

        StringBuilder buffer = new StringBuilder();
        String line;
        boolean started = false;
        while ((line = reader.readLine()) != null) {
            line = ConsoleNote.removeNotes(line);

            if (startPattern.matcher(line).matches()) {
                started = true;
                continue;
            }
            if (endPattern.matcher(line).matches()) break;

            if (started) buffer.append(line).append('\n');
        }
        return buffer.toString();
    }
}
