package hudson.plugins.emailext.plugins.content;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.Run;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import java.util.ArrayList;
import java.util.List;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

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

    @Whitelisted
    public Action getCoberturaAction() {
        return getAction("hudson.plugins.cobertura.CoberturaBuildAction");
    }

    @Whitelisted
    public List<TestResult> getJUnitTestResult() {
        List<TestResult> result = new ArrayList<>();
        List<AggregatedTestResultAction> actions = build.getActions(AggregatedTestResultAction.class);
        for (AggregatedTestResultAction action : actions) {
            /* Maven Project */
            List<AggregatedTestResultAction.ChildReport> reportList = action.getChildReports();
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
