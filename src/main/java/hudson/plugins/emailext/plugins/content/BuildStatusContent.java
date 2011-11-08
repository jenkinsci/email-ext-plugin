package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BuildStatusContent implements EmailContent {

    private static final String TOKEN = "BUILD_STATUS";

    public String getToken() {
        return TOKEN;
    }

    public List<String> getArguments() {
        return Collections.emptyList();
    }

    public String getHelpText() {
        return "Displays the status of the current build. (failing, success, etc...)";
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher,
            EmailType emailType, Map<String, ?> args) {

        // Build can be "building" when the pre-build trigger is used. (and in this case there is not result set yet for the build)
        // Reporting "success", "still failing", etc doesn't make sense in this case.
		// When using on matrix build, the build is still in building stage when matrix aggregator end build trigger is fired, though 
        if ( (build.isBuilding()) && (null == build.getResult())) {
            return "Building";
        }

        Result buildResult = build.getResult();
        if (buildResult == Result.FAILURE) {
            B prevBuild = build.getPreviousBuild();
            if (prevBuild != null && (prevBuild.getResult() == Result.FAILURE)) {
                return "Still Failing";
            } else {
                return "Failure";
            }
        } else if (buildResult == Result.UNSTABLE) {
            B prevBuild = build.getPreviousBuild();
            if (prevBuild != null) {
               if (prevBuild.getResult() == Result.UNSTABLE) {
                  return "Still Unstable";
               } else if (prevBuild.getResult() == Result.SUCCESS) {
                  return "Unstable";
               } else if (prevBuild.getResult() == Result.FAILURE ||
                  prevBuild.getResult() == Result.ABORTED ||
                  prevBuild.getResult() == Result.NOT_BUILT) {
                  //iterate through previous builds
                  //(fail_or_aborted)* and then an unstable : return still unstable
                  //(fail_or_aborted)* and then successful : return unstable
                  B previous = prevBuild.getPreviousBuild();
                  while (previous != null) {
                     if (previous.getResult() == Result.SUCCESS) {
                        return "Unstable";
                     }
                     if (previous.getResult() == Result.UNSTABLE) {
                        return "Still unstable";
                     }
                     previous = previous.getPreviousBuild();
                  }
                  return "Unstable";
               }
            } else {
                return "Unstable";
            }
        } else if (buildResult == Result.SUCCESS) {
            B prevBuild = build.getPreviousBuild();
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

    public boolean hasNestedContent() {
        return false;
    }
}
