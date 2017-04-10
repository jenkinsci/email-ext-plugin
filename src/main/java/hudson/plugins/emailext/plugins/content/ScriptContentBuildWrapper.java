package hudson.plugins.emailext.plugins.content;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.Run;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptContentBuildWrapper {

    private Run<?, ?> build;

    public ScriptContentBuildWrapper(Run<?, ?> build) {
        this.build = build;
    }

    @Whitelisted
    public String getTimestampString() {
        return Functions.rfc822Date(build.getTimestamp());
    }

    public Action getAction(String className) {
        for (Action a : build.getAllActions()) {
            if (a.getClass().getName().equals(className)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Returns all build actions that derive from
     * <code>AbstractResultAction</code>. Every action represents a single
     * analysis result.
     * 
     * @return The static analysis actions for the current build. The returned
     *         list might be empty if there are no such actions.
     */
    @Whitelisted
    public List<Action> getStaticAnalysisActions() {
        if (isPluginInstalled("analysis-core")) {
            return new StaticAnalysisUtilities().getActions(build);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns whether the specified plug-in is installed.
     *
     * @param shortName
     *            the plug-in to check
     * @return <code>true</code> if the specified plug-in is installed,
     *         <code>false</code> if not.
     */
    public static boolean isPluginInstalled(final String shortName) {
        return Jenkins.getActiveInstance().getPlugin(shortName) != null;
    }

    @Whitelisted
    public Action getCoberturaAction() {
        return getAction("hudson.plugins.cobertura.CoberturaBuildAction");
    }

    @Whitelisted
    public List<TestResult> getJUnitTestResult() {
        List<TestResult> result = new ArrayList<>();
        List<AggregatedTestResultAction> actions = build.getActions(AggregatedTestResultAction.class);
        for (Action action : actions) {
            /* Maven Project */
            List<AggregatedTestResultAction.ChildReport> reportList =
                    ((AggregatedTestResultAction) action).getChildReports();
            for (AggregatedTestResultAction.ChildReport report : reportList) {
                if (report.result instanceof hudson.tasks.junit.TestResult) {
                    result.add((TestResult) report.result);
                }
            }
        }

        if (result.isEmpty()) {
            /*FreestyleProject*/
            TestResultAction action = build.getAction(TestResultAction.class);
            if (action != null) {
                result.add(action.getResult());
            }
        }
        return result;
    }
}
