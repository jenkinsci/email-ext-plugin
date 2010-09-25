package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.Util;
import hudson.plugins.emailext.Util.PrintfSpec;
import hudson.plugins.emailext.plugins.EmailContent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangesSinceLastSuccessfulBuildContent
    extends AbstractChangesSinceContent
{
    private static final String TOKEN = "CHANGES_SINCE_LAST_SUCCESS";

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
        return "Displays the changes since the last successful build.";
    }

    @Override
    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> AbstractBuild<P, B> getFirstIncludedBuild(
        AbstractBuild<P, B> build )
    {
        AbstractBuild<P, B> firstIncludedBuild = build;

        B prev = firstIncludedBuild.getPreviousBuild();
        while ( prev != null && prev.getResult() != Result.SUCCESS )
        {
            firstIncludedBuild = prev;
            prev = firstIncludedBuild.getPreviousBuild();
        }

        return firstIncludedBuild;
    }
}
