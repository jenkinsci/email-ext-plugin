/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext.plugins.recipients;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.internet.InternetAddress;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author acearl
 */

public class CulpritsRecipientProvider extends RecipientProvider {
    
    @DataBoundConstructor
    public CulpritsRecipientProvider() {
        
    }
    
    @Override
    public void addRecipients(final ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor
                    = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }
        final Debug debug = new Debug();
        Run<?,?> run = context.getRun();

        boolean runHasGetCulprits = false;

        // TODO: core 2.60+, workflow-job 2.12+: Switch to checking if run is RunWithSCM and make catch an else block
        try {
            Method getCulprits = run.getClass().getMethod("getCulprits");
            runHasGetCulprits = true;
            if (Set.class.isAssignableFrom(getCulprits.getReturnType())) {
                @SuppressWarnings("unchecked")
                Set<User> users = (Set<User>) getCulprits.invoke(run);
                if (Iterables.all(users, Predicates.instanceOf(User.class))) {
                    RecipientProviderUtilities.addUsers(users, context, env, to, cc, bcc, debug);
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            // Only log a debug message if the exception is not due to an older WorkflowRun without getCulprits...
            if (!(run instanceof WorkflowRun && !runHasGetCulprits)) {
                debug.send("Exception getting culprits for %s: %s", run, e);
            }
            List<Run<?, ?>> builds = new ArrayList<>();
            Run<?, ?> build = run;
            builds.add(build);
            build = build.getPreviousCompletedBuild();
            while (build != null) {
                final Result buildResult = build.getResult();
                if (buildResult != null) {
                    if (buildResult.isWorseThan(Result.SUCCESS)) {
                        debug.send("Including build %s with status %s", build.getId(), buildResult);
                        builds.add(build);
                    } else {
                        break;
                    }
                }
                build = build.getPreviousCompletedBuild();
            }
            Set<User> users = RecipientProviderUtilities.getChangeSetAuthors(builds, debug);
            RecipientProviderUtilities.addUsers(users, context, env, to, cc, bcc, debug);
        }
    }

    @Extension
    @Symbol("culprits")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        
        @Override
        public String getDisplayName() {
            return "Culprits";
        }
        
    }
    
}
