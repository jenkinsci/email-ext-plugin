package hudson.plugins.emailext.plugins.content;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailToken;

@EmailToken
public class ChangesSinceLastUnstableBuildContent
        extends AbstractChangesSinceContent {

    public static final String MACRO_NAME = "CHANGES_SINCE_LAST_UNSTABLE";
    private static final String FORMAT_DEFAULT_VALUE = "Changes for Build #%n\\n%c\\n";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String getDefaultFormatValue() {
        return FORMAT_DEFAULT_VALUE;
    }

    @Override
    public String getShortHelpDescription() {
        return "Expands to the changes since the last unstable or successful build. ";
    }

    @Override
    public Run<?,?> getFirstIncludedBuild(Run<?,?> build, TaskListener listener) {
        Run<?,?> firstIncludedBuild = build;

        Run<?,?> prev = ExtendedEmailPublisher.getPreviousBuild(firstIncludedBuild, listener);
        while (prev != null && prev.getResult().isWorseThan(Result.UNSTABLE)) {
            firstIncludedBuild = prev;
            prev = ExtendedEmailPublisher.getPreviousBuild(firstIncludedBuild, listener);
        }

        return firstIncludedBuild;
    }
}
