package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;

public class ChangesSinceLastUnstableBuildContent
    extends AbstractChangesSinceContent
{
    private static final String TOKEN = "CHANGES_SINCE_LAST_UNSTABLE";

    private static final String FORMAT_DEFAULT_VALUE = "Changes for Build #%n\\n%c\\n";

    public String getToken()
    {
        return TOKEN;
    }

    @Override
    public String getDefaultFormatValue()
    {
        return FORMAT_DEFAULT_VALUE;
    }

    @Override
    public String getShortHelpDescription()
    {
        return "Displays the changes since the last unstable or successful build.";
    }

    @Override
    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> AbstractBuild<P, B> getFirstIncludedBuild(
        AbstractBuild<P, B> build )
    {
        AbstractBuild<P, B> firstIncludedBuild = build;

			B prev = firstIncludedBuild.getPreviousBuild();
			while (prev != null && prev.getResult().isWorseThan(Result.UNSTABLE)) {
				firstIncludedBuild = prev;
				prev = firstIncludedBuild.getPreviousBuild();
			}

        return firstIncludedBuild;
    }
}
