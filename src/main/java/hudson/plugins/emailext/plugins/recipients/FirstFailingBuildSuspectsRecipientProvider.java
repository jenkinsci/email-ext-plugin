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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.Messages;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

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
                = Jenkins.get().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

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
            if (!Objects.equals(currentRun.getResult(), Result.FAILURE)) {
                debug.send("currentBuild did not fail");
            } else {
                users = new HashSet<>();
                debug.send("Collecting builds with suspects...");
                final HashSet<Run<?, ?>> buildsWithSuspects = new HashSet<>();
                Run<?, ?> firstFailedBuild = currentRun;
                Run<?, ?> candidate = currentRun;
                while (candidate != null) {
                    final Result candidateResult = candidate.getResult();
                    if ( candidateResult == null || !candidateResult.isWorseOrEqualTo(Result.FAILURE) ) {
                        break;
                    }
                    firstFailedBuild = candidate;
                    candidate = candidate.getPreviousCompletedBuild();
                }
                buildsWithSuspects.add(firstFailedBuild);
                debug.send("Collecting suspects...");
                users.addAll(RecipientProviderUtilities.getChangeSetAuthors(buildsWithSuspects, debug));
                users.addAll(RecipientProviderUtilities.getUsersTriggeringTheBuilds(buildsWithSuspects, debug));
            }
        }
        if (users != null) {
            RecipientProviderUtilities.addUsers(users, context, env, to, cc, bcc, debug);
        }
    }

    @Extension
    @Symbol("brokenBuildSuspects")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.FirstFailingBuildSuspectsRecipientProvider_DisplayName();
        }
    }
}
