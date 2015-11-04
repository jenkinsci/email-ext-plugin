/*
 * The MIT License
 *
 * Copyright (c) 2014 Stellar Science Ltd Co, K. R. Walker
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.emailext.plugins.recipients;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import jenkins.model.Jenkins;

/**
 * A recipient provider that assigns ownership of a failing build to the set of developers (including any initiator)
 * that committed changes that first broke the build.
 */
public class FirstFailingBuildSuspectsRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public FirstFailingBuildSuspectsRecipientProvider() {
    }

    @Override
    public void addRecipients(final ExtendedEmailPublisherContext context, final EnvVars env,
        final Set<InternetAddress> to, final Set<InternetAddress> cc, final Set<InternetAddress> bcc) {

        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor
                = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }
        final Debug debug = new Debug();

        Set<User> users = null;

        final Run<?, ?> currentRun = context.getRun();
        if (currentRun == null) {
            debug.send("currentRun was null");
        } else {
            if (!currentRun.getResult().equals(Result.FAILURE)) {
                debug.send("currentBuild did not fail");
            } else {
                users = new HashSet<User>();
                debug.send("Collecting builds with suspects...");
                final HashSet<Run<?, ?>> buildsWithSuspects = new HashSet<Run<?, ?>>();
                Run<?, ?> firstFailedBuild = currentRun;
                Run<?, ?> candidate = currentRun;
                while (candidate != null && candidate.getResult().isWorseOrEqualTo(Result.FAILURE)) {
                    firstFailedBuild = candidate;
                    candidate = candidate.getPreviousCompletedBuild();
                }
                if (firstFailedBuild instanceof AbstractBuild) {
                    buildsWithSuspects.add(firstFailedBuild);
                } else {
                    debug.send("  firstFailedBuild was not an instance of AbstractBuild");
                }
                debug.send("Collecting suspects...");
                users.addAll(RecipientProviderUtilities.getChangeSetAuthors(buildsWithSuspects, debug));
                users.addAll(RecipientProviderUtilities.getUsersTriggeringTheBuilds(buildsWithSuspects, debug));
            }
        }
        if (users != null) {
            RecipientProviderUtilities.addUsers(users, context.getListener(), env, to, cc, bcc, debug);
        }
    }

    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        @Override
        public String getDisplayName() {
            return "Suspects Causing the Build to Begin Failing";
        }
    }

}
