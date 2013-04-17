package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import hudson.plugins.emailext.Util;
import hudson.scm.ChangeLogSet;
import java.io.IOException;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map.Entry;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.sonatype.aether.util.StringUtils;

@EmailToken
public class ChangesSinceLastBuildContent extends DataBoundTokenMacro {
    
    public static final String FORMAT_DEFAULT_VALUE = "[%a] %m\\n";
    public static final String PATH_FORMAT_DEFAULT_VALUE = "\\t%p\\n";
    public static final String FORMAT_DEFAULT_VALUE_WITH_PATHS = "[%a] %m%p\\n";

    public static final String MACRO_NAME = "CHANGES";

    @Parameter
    public boolean showPaths = false;
    @Parameter
    public String format;
    @Parameter 
    public String pathFormat = PATH_FORMAT_DEFAULT_VALUE;
    @Parameter
    public boolean showDependencies = false;    
    
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
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        if(StringUtils.isEmpty(format)) {
            format = showPaths ? FORMAT_DEFAULT_VALUE_WITH_PATHS : FORMAT_DEFAULT_VALUE;
        }
        StringBuffer buf = new StringBuffer();
        for (ChangeLogSet.Entry entry : build.getChangeSet()) {
            Util.printf(buf, format, new ChangesSincePrintfSpec(entry, pathFormat));
        }
        
        if (showDependencies && build.getPreviousBuild() != null)
            for (Entry<AbstractProject, DependencyChange> e : build
                    .getDependencyChanges(build.getPreviousBuild()).entrySet()) {
                buf.append("\n=======================\n");
                buf.append("\nChanges in ").append(e.getKey().getName())
                        .append(":\n");
                for (AbstractBuild<?, ?> b : e.getValue().getBuilds()) {
                    for (ChangeLogSet.Entry entry : b.getChangeSet()) {
                        Util.printf(buf, format,
                                new ChangesSincePrintfSpec(entry,
                                        pathFormat));
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

        public ChangesSincePrintfSpec(ChangeLogSet.Entry entry, String pathFormatString) {
            this.entry = entry;
            this.pathFormatString = pathFormatString;
        }

        public boolean printSpec(StringBuffer buf, char formatChar) {
            switch (formatChar) {
                case 'a':
                    buf.append(entry.getAuthor().getFullName());
                    return true;
                case 'd': {
                    try {
                        Method getDateMethod = entry.getClass().getMethod("getDate");
                        buf.append(getDateMethod.invoke(entry));
                    } catch (Exception e) {
                        // If it is not implemented or any other problem, swallow the %d
                    }
                    return true;
                }
                case 'm': {
                    String m = entry.getMsg();
                    buf.append(m);
                    if (!m.endsWith("\n")) {
                        buf.append('\n');
                    }
                    return true;
                }
                case 'p': {
                    Collection<String> affectedPaths = entry.getAffectedPaths();
                    for (final String affectedPath : affectedPaths) {
                        Util.printf(buf, pathFormatString, new Util.PrintfSpec() {
                            public boolean printSpec(StringBuffer buf, char formatChar) {
                                if (formatChar == 'p') {
                                    buf.append(affectedPath);
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        });
                    }
                    return true;
                }
                case 'r': {
                    try {
                        Method getRevisionMethod = entry.getClass().getMethod("getRevision");
                        buf.append(getRevisionMethod.invoke(entry));
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
