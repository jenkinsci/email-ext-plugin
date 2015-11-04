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

import hudson.model.*;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.Util;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

abstract public class AbstractChangesSinceContent
        extends DataBoundTokenMacro {

    @Parameter
    public boolean reverse = false;
    @Parameter
    public String format;
    @Parameter
    public boolean showPaths = false;
    @Parameter
    public String changesFormat;
    @Parameter
    public String pathFormat = "\\t%p\\n";
    @Parameter
    public boolean showDependencies = false;
    @Parameter
    public String dateFormat;
    @Parameter
    public String regex;
    @Parameter
    public String replace;
    @Parameter(alias="default")
    public String def = ChangesSinceLastBuildContent.DEFAULT_DEFAULT_VALUE;

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        // No previous build so bail
        if (ExtendedEmailPublisher.getPreviousRun(build, listener) == null) {
            return "";
        }

        if (StringUtils.isEmpty(format)) {
            format = getDefaultFormatValue();
        }

        StringBuffer sb = new StringBuffer();
        final Run startBuild;
        final Run endBuild;
        if (reverse) {
            startBuild = build;
            endBuild = getFirstIncludedRun(build, listener);
        } else {
            startBuild = getFirstIncludedRun(build, listener);
            endBuild = build;
        }
        Run<?, ?> currentBuild = null;
        while (currentBuild != endBuild) {
            if (currentBuild == null) {
                currentBuild = startBuild;
            } else {
                if (reverse) {
                    currentBuild = currentBuild.getPreviousBuild();
                } else {
                    currentBuild = currentBuild.getNextBuild();
                }
            }
            appendBuild(sb, listener, currentBuild);
        }

        return sb.toString();
    }

    private void appendBuild(StringBuffer buf,
            final TaskListener listener,
            final Run<?, ?> currentRun)
            throws MacroEvaluationException {
        // Use this object since it already formats the changes per build
        final ChangesSinceLastBuildContent changes = new ChangesSinceLastBuildContent(changesFormat, pathFormat, showPaths);
        changes.showDependencies = showDependencies;
        changes.dateFormat = dateFormat;
        changes.regex = regex;
        changes.replace = replace;
        changes.def = def;

        Util.printf(buf, format, new Util.PrintfSpec() {
            public boolean printSpec(StringBuffer buf, char formatChar) {
                switch (formatChar) {
                    case 'c':
                        try {
                            if(currentRun instanceof AbstractBuild) {
                                buf.append(changes.evaluate((AbstractBuild) currentRun, listener, ChangesSinceLastBuildContent.MACRO_NAME));
                            }
                        } catch(Exception e) {
                            // do nothing
                        }
                        return true;
                    case 'n':
                        buf.append(currentRun.getNumber());
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    @Override
    public boolean hasNestedContent() {
        return true;
    }

    public abstract String getDefaultFormatValue();

    public abstract String getShortHelpDescription();

    public abstract Run<?,?> getFirstIncludedRun(Run<?,?> build, TaskListener listener);
}
