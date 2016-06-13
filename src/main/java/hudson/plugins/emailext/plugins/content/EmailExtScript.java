package hudson.plugins.emailext.plugins.content;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import groovy.lang.Script;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.ContentBuilder;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EmailExtScript extends Script {

    private void populateArgs(Object args, Map<String, String> map, ListMultimap<String, String> multiMap) {
        if(args instanceof Object[]) {
            Object[] argArray = (Object[])args;
            if(argArray.length > 0) {
                Map<String, Object> argMap = (Map<String, Object>)argArray[0];
                for(Map.Entry<String, Object> entry : argMap.entrySet()) {
                    String value = entry.getValue().toString();
                    if(entry.getValue() instanceof List) {
                        List valueList = (List)entry.getValue();
                        for(Object v : valueList) {
                            multiMap.put(entry.getKey(), v.toString());
                        }
                        value = valueList.get(valueList.size() - 1).toString();
                    } else {
                        multiMap.put(entry.getKey(), value);
                    }                                               
                    map.put(entry.getKey(), value);
                }                    
            }                
        }
    }

    public Object methodMissing(String name, Object args)
            throws MacroEvaluationException, IOException, InterruptedException {
    
        TokenMacro macro = null;
        for(TokenMacro m : TokenMacro.all()) {
            if(m.acceptsMacroName(name)) {
                macro = m;
                break;
            }
        }

		if (macro == null) {
            for(TokenMacro m : ContentBuilder.getPrivateMacros()) {
                if(m.acceptsMacroName(name)) {
                    macro = m;
                    break;
                }
            }
        }

        if (macro != null) {
            Map<String, String> argsMap = new HashMap<>();
            ListMultimap<String, String> argsMultimap = ArrayListMultimap.create();
            populateArgs(args, argsMap, argsMultimap);

            // Get the build and listener from the binding.
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>)this.getBinding().getVariable("build");
            TaskListener listener = (TaskListener)this.getBinding().getVariable("listener");

            return macro.evaluate(build, listener, name, argsMap, argsMultimap);
        } else {
            // try environment variables

            // Get the build and listener from the binding.
            Run<?,?> build = (Run<?,?>)this.getBinding().getVariable("build");
            TaskListener listener = (TaskListener)this.getBinding().getVariable("listener");
            EnvVars vars = build.getEnvironment(listener);
            if(vars.containsKey(name)) {
                return vars.get(name, "");
            }
        }

        return String.format("[Could not find content token (check your usage): %s]", name);
    }
}
