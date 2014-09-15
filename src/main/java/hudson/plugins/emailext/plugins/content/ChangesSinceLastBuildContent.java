package hudson.plugins.emailext.plugins.content;

import hudson.FilePath;
import hudson.model.DependencyChange;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.Util;
import hudson.plugins.emailext.plugins.EmailToken;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;

@EmailToken
public class ChangesSinceLastBuildContent extends DataBoundTokenMacro {

    public static final String FORMAT_DEFAULT_VALUE = "[%a] %m\\n";
    public static final String PATH_FORMAT_DEFAULT_VALUE = "\\t%p\\n";
    public static final String FORMAT_DEFAULT_VALUE_WITH_PATHS = "[%a] %m%p\\n";
    public static final String DEFAULT_DEFAULT_VALUE = "No changes\n";
    public static final String MACRO_NAME = "CHANGES";
    @Parameter
    public boolean showPaths = false;
    @Parameter
    public String format;
    @Parameter
    public String pathFormat = PATH_FORMAT_DEFAULT_VALUE;
    @Parameter
    public boolean showDependencies = false;
    @Parameter
    public String dateFormat;
    @Parameter
    public String regex;
    @Parameter
    public String replace;
    @Parameter(alias="default")
    public String def = DEFAULT_DEFAULT_VALUE;

    public ChangesSinceLastBuildContent() {

    }

    public ChangesSinceLastBuildContent(String format, String pathFormat, boolean showPaths) {
        this.format = format;
        this.pathFormat = pathFormat;
        this.showPaths = showPaths;
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(Run<?, ?> build, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        if (StringUtils.isEmpty(format)) {
            format = showPaths ? FORMAT_DEFAULT_VALUE_WITH_PATHS : FORMAT_DEFAULT_VALUE;
        }

        DateFormat dateFormatter;
        if (StringUtils.isEmpty(dateFormat)) {
            dateFormatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
        } else {
            dateFormatter = new SimpleDateFormat(dateFormat);
        }

        StringBuffer buf = new StringBuffer();
        ChangeLogSet<? extends ChangeLogSet.Entry> changeset = build.getChangeSet();

        // TODO: what if there are multiple SCMs?
        if (changeset != null && !changeset.isEmptySet()) {
            for (ChangeLogSet.Entry entry : changeset) {
                Util.printf(buf, format, new ChangesSincePrintfSpec(entry,
                        pathFormat, dateFormatter));
            }
        } else {
            buf.append(def);
        }

        if (showDependencies) {
            Run previousBuild = ExtendedEmailPublisher.getPreviousBuild(build, listener);
            if (previousBuild != null) {
                for (Entry<Job, DependencyChange> e : build.getDependencyChanges(previousBuild).entrySet()) {
                    buf.append("\n=======================\n");
                    buf.append("\nChanges in ").append(e.getKey().getName())
                            .append(":\n");
                    for (Run<?, ?> b : e.getValue().getBuilds()) {
                        if (!b.getChangeSet().isEmptySet()) {
                            for (ChangeLogSet.Entry entry : b.getChangeSet()) {
                                Util.printf(buf, format,
                                        new ChangesSincePrintfSpec(entry,
                                                pathFormat, dateFormatter));
                            }
                        } else {
                            buf.append(def);
                        }
                    }
                }
            }
        }

        return buf.toString();
    }

    @Override
    public boolean hasNestedContent() {
        return true;
    }

    public class ChangesSincePrintfSpec
            implements Util.PrintfSpec {

        final private ChangeLogSet.Entry entry;
        final private String pathFormatString;
        final private DateFormat dateFormatter;

        public ChangesSincePrintfSpec(ChangeLogSet.Entry entry, String pathFormatString, DateFormat dateFormatter) {
            this.entry = entry;
            this.pathFormatString = pathFormatString;
            this.dateFormatter = dateFormatter;
        }

        public boolean printSpec(StringBuffer buf, char formatChar) {
            switch (formatChar) {
                case 'a':
                    buf.append(entry.getAuthor().getFullName());
                    return true;
                case 'd': {
                    try {
                        buf.append(dateFormatter.format(new Date(entry.getTimestamp())));
                    } catch (Exception e) {
                        // If it is not implemented or any other problem, swallow the %d
                    }
                    return true;
                }
                case 'm': {
                    String m = entry.getMsg();
                    if(!StringUtils.isEmpty(regex) && !StringUtils.isEmpty(replace)) {
                        m = m.replaceAll(regex, replace);
                    }
                    buf.append(m);
                    if (m == null || !m.endsWith("\n")) {
                        buf.append('\n');
                    }
                    return true;
                }
                case 'p': {
                    try {
                        Collection<? extends AffectedFile> affectedFiles = entry.getAffectedFiles();
                        for (final AffectedFile file : affectedFiles) {
                            Util.printf(buf, pathFormatString, new Util.PrintfSpec() {
                                public boolean printSpec(StringBuffer buf, char formatChar) {
                                    if (formatChar == 'p') {
                                        buf.append(file.getPath());
                                        return true;
                                    } else if(formatChar == 'a') {
                                        buf.append(file.getEditType().getName());
                                        return true;
                                    } else if(formatChar == 'd') {
                                        buf.append(file.getEditType().getDescription());
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                            });
                        }
                    } catch(UnsupportedOperationException e) {
                        Collection<String> affectedPaths = entry.getAffectedPaths();
                        for (final String affectedPath : affectedPaths) {
                            Util.printf(buf, pathFormatString, new Util.PrintfSpec() {
                                public boolean printSpec(StringBuffer buf, char formatChar) {
                                    if (formatChar == 'p') {
                                        buf.append(affectedPath);
                                        return true;
                                    } else if(formatChar == 'a') {
                                        buf.append("Unknown");
                                        return true;
                                    } else if(formatChar == 'd') {
                                        buf.append("Unknown");
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                            });
                        }
                    }
                    return true;
                }                
                case 'r': {
                    try {
                        buf.append(entry.getCommitId());
                    } catch (Exception e) {
                        // If it is not implemented or any other problem, swallow the %r
                    }
                    return true;
                }
                default:
                    return false;
            }
        }
    }
}
