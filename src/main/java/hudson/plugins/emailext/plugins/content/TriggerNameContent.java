package hudson.plugins.emailext.plugins.content;

import com.google.common.collect.ListMultimap;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Map;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

public class TriggerNameContent extends TokenMacro {
    private static final String MACRO_NAME = "TRIGGER_NAME";
    private final String name;
    
    public TriggerNameContent(String name) {
        this.name = name;
    }
    
    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> ab, TaskListener tl, String string, Map<String, String> map, ListMultimap<String, String> lm) throws MacroEvaluationException, IOException, InterruptedException {
        return name;
    }
}
