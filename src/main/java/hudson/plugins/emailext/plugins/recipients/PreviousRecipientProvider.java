/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext.plugins.recipients;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.Messages;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;

/**
 * A recipient provider that finds the first culprits / requestor or developers of the previous build(s).
 * 
 * @author strangelookingnerd
 */
public class PreviousRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public PreviousRecipientProvider() {

    }

    @Override
    public void addRecipients(final ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to,
            Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor = Jenkins.get()
                    .getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            @Override
            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }

        final Debug debug = new Debug();
        Run<?, ?> run = context.getRun();
        Set<User> users = new HashSet<>();

        if (run instanceof RunWithSCM) {
            users.addAll(((RunWithSCM<?, ?>) run).getCulprits());
        }

        Run<?, ?> build = run;

        while (users.isEmpty() && build != null) {
            users.addAll(RecipientProviderUtilities.getChangeSetAuthors(Collections.singleton(build), debug));
            users.addAll(RecipientProviderUtilities.getUsersTriggeringTheBuilds(Collections.singleton(build), debug));
            build = build.getPreviousCompletedBuild();
        }

        RecipientProviderUtilities.addUsers(users, context, env, to, cc, bcc, debug);
    }

    @Extension
    @Symbol("previous")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.PreviousRecipientProvider_DisplayName();
        }
    }
}
