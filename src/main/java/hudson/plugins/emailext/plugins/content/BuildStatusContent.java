package hudson.plugins.emailext.plugins.content;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension
public class BuildStatusContent extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "BUILD_STATUS";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        // Build can be "building" when the pre-build trigger is used. (and in this case there is not result set yet for the build)
        // Reporting "success", "still failing", etc doesn't make sense in this case.
        // When using on matrix build, the build is still in building stage when matrix aggregator end build trigger is fired, though 
        if ( (build.isBuilding()) && (null == build.getResult())) {
            return "Building";
        }

        Result buildResult = build.getResult();
        if (buildResult == Result.FAILURE) {
            Run<?,?> prevBuild = ExtendedEmailPublisher.getPreviousRun(build, listener);
            if (prevBuild != null && (prevBuild.getResult() == Result.FAILURE)) {
                return "Still Failing";
            } else {
                return "Failure";
            }
        } else if (buildResult == Result.UNSTABLE) {
            Run<?,?> prevRun = ExtendedEmailPublisher.getPreviousRun(build, listener);
            if (prevRun != null) {
               if (prevRun.getResult() == Result.UNSTABLE) {
                  return "Still Unstable";
               } else if (prevRun.getResult() == Result.SUCCESS) {
                  return "Unstable";
               } else if (prevRun.getResult() == Result.FAILURE ||
                  prevRun.getResult() == Result.ABORTED ||
                  prevRun.getResult() == Result.NOT_BUILT) {
                  //iterate through previous builds
                  //(fail_or_aborted)* and then an unstable : return still unstable
                  //(fail_or_aborted)* and then successful : return unstable
                  Run<?,?> previous = ExtendedEmailPublisher.getPreviousRun(prevRun, listener);
                  while (previous != null) {
                     if (previous.getResult() == Result.SUCCESS) {
                        return "Unstable";
                     }
                     if (previous.getResult() == Result.UNSTABLE) {
                        return "Still unstable";
                     }
                     previous = ExtendedEmailPublisher.getPreviousRun(previous, listener);
                  }
                  return "Unstable";
               }
            } else {
                return "Unstable";
            }
        } else if (buildResult == Result.SUCCESS) {
            Run<?,?> prevBuild = ExtendedEmailPublisher.getPreviousRun(build, listener);
            if (prevBuild != null && (prevBuild.getResult() == Result.UNSTABLE || prevBuild.getResult() == Result.FAILURE)) {
                return "Fixed";
            } else {
                return "Successful";
            }
        } else if (buildResult == Result.NOT_BUILT) {
            return "Not Built";
        } else if (buildResult == Result.ABORTED) {
            return "Aborted";
        }

        return "Unknown";
    }
}
