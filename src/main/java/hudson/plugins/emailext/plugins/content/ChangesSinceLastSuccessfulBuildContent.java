package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.plugins.emailext.plugins.EmailToken;

@EmailToken
public class ChangesSinceLastSuccessfulBuildContent
        extends AbstractChangesSinceContent {

    public static final String MACRO_NAME = "CHANGES_SINCE_LAST_SUCCESS";
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
        return "Displays the changes since the last successful build.";
    }

    @Override
    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> AbstractBuild<P, B> getFirstIncludedBuild(
            AbstractBuild<P, B> build) {
        AbstractBuild<P, B> firstIncludedBuild = build;

        B prev = firstIncludedBuild.getPreviousBuild();
        while (prev != null && prev.getResult() != Result.SUCCESS) {
            firstIncludedBuild = prev;
            prev = firstIncludedBuild.getPreviousBuild();
        }

        return firstIncludedBuild;
    }
}
