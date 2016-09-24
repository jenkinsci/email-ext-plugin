package hudson.plugins.emailext.plugins.content;

import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.analysis.core.AbstractResultAction;
import hudson.plugins.analysis.core.MavenResultAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides some helper methods to integrate the static analysis plug-ins into
 * email-ext. Methods of this class will throw {@link ClassNotFoundException} if
 * the analysis-core plug-in is not installed.
 *
 * @author Ulli Hafner
 */
public class StaticAnalysisUtilities {

    /**
     * Returns all build actions that derive from {@link AbstractResultAction}.
     * Every action represents a single analysis result.
     *
     * @param build the build to get the actions for
     * @return The static analysis actions for the specified build. The returned
     * list might be empty if there are no such actions.
     */
    public List<Action> getActions(Run<?, ?> build) {
        ArrayList<Action> actions = new ArrayList<>();
        for (Action action : build.getActions(Action.class)) {
            if (AbstractResultAction.class.isInstance(action) || MavenResultAction.class.isInstance(action)) {
                actions.add(action);
            }
        }
        return actions;
    }
}
