package hudson.plugins.emailext.plugins.content;

import hudson.Functions;
import hudson.maven.reporters.SurefireAggregatedReport;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;

import java.util.ArrayList;
import java.util.List;

public class JellyScriptContentBuildWrapper
{
    private AbstractBuild<?, ?> build;

    public JellyScriptContentBuildWrapper( AbstractBuild<?, ?> build )
    {
        this.build = build;
    }

    public String getTimestampString()
    {
        return Functions.rfc822Date( build.getTimestamp() );
    }

    public Action getAction( String className )
    {
        for ( Action a : build.getActions() )
        {
            if ( a.getClass().getName().equals( className ) )
            {
                return a;
            }
        }
        return null;
    }

    public Action getCoberturaAction()
    {
        return getAction( "hudson.plugins.cobertura.CoberturaBuildAction" );
    }

    public List<TestResult> getJUnitTestResult()
    {
        List<TestResult> result = new ArrayList<TestResult>();
        List<Action> actions = build.getActions();
        for ( Action action : actions )
        {
            if ( action instanceof hudson.maven.reporters.SurefireAggregatedReport )
            {
                /* Maven Project */
                List<AggregatedTestResultAction.ChildReport> reportList =
                    ( (SurefireAggregatedReport) action ).getChildReports();
                for ( AggregatedTestResultAction.ChildReport report : reportList )
                {
                    if ( report.result instanceof hudson.tasks.junit.TestResult )
                    {
                        result.add( (TestResult) report.result );
                    }
                }
            }
        }

        if ( result.isEmpty() )
        {
            /*FreestyleProject*/
            TestResultAction action = build.getAction( TestResultAction.class );
            if ( action != null )
            {
                result.add( action.getResult() );
            }
        }
        return result;
    }
}
