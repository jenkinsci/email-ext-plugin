/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext.plugins.recipients;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.EnvVars;
import hudson.Extension;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

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
        if (run instanceof AbstractBuild) {
            Set<User> users = ((AbstractBuild<?,?>)run).getCulprits();
            RecipientProviderUtilities.addUsers(users, context.getListener(), env, to, cc, bcc, debug);
        } else if (run.getResult() != null && run.getResult().isWorseThan(Result.SUCCESS)) {
            List<Run<?, ?>> builds = new ArrayList<>();
            Run<?, ?> build = run;
            while (build != null) {
                if (build.getResult() != null) {
                    if (build.getResult().isWorseThan(Result.SUCCESS)) {
                        builds.add(build);
                    } else {
                        break;
                    }
                }
                build = build.getPreviousCompletedBuild();
            }
            Set<User> users = RecipientProviderUtilities.getChangeSetAuthors(builds, debug);
            RecipientProviderUtilities.addUsers(users, context.getListener(), env, to, cc, bcc, debug);
        }
    }

    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        
        @Override
        public String getDisplayName() {
            return "Culprits";
        }
        
    }
    
}
