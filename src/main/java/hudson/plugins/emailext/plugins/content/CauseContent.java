package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.TaskListener;
import hudson.plugins.emailext.EmailToken;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedList;
import java.util.List;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@EmailToken
public class CauseContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "CAUSE";
    
    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }
    
    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        List<Cause> causes = new LinkedList<Cause>();
        CauseAction causeAction = build.getAction(CauseAction.class);
        if (causeAction != null) {
            causes = causeAction.getCauses();
        }

        return formatCauses(causes);
    }

    private String formatCauses(List<Cause> causes) {
        if (causes.isEmpty()) {
            return "N/A";
        }

        List<String> causeNames = new LinkedList<String>();
        for (Cause cause : causes) {
            causeNames.add(cause.getShortDescription());
        }

        return StringUtils.join(causeNames, ", ");
    }
}
