package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailToken;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.util.List;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * An EmailContent for build log. Shows last 250 lines of the build log file.
 * 
 * @author dvrzalik
 */
@EmailToken
public class BuildLogContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "BUILD_LOG";
    
    public static final int MAX_LINES_DEFAULT_VALUE = 250;

    @Parameter
    public int maxLines = MAX_LINES_DEFAULT_VALUE;

    @Parameter
    public boolean escapeHtml = false;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        StringBuilder buffer = new StringBuilder();
        try {
            List<String> lines = build.getLog(maxLines);
            for (String line : lines) {
                if (escapeHtml) {
                    line = StringEscapeUtils.escapeHtml(line);
                }
                buffer.append(line);
                buffer.append('\n');
            }
        } catch (IOException e) {
            listener.getLogger().append("Error getting build log data: " + e.getMessage());
        }

        return buffer.toString();
    }
}
