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
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A recipient provider that assigns ownership of a failing test to the set of developers (including any initiator)
 * that committed changes that first broke the test.
 */
public class FailingTestSuspectsRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public FailingTestSuspectsRecipientProvider() {
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
            final AbstractTestResultAction<?> testResultAction = currentRun.getAction(AbstractTestResultAction.class);
            if (testResultAction == null) {
                debug.send("testResultAction was null");
            } else {
                if (testResultAction.getFailCount() <= 0) {
                    debug.send("getFailCount() returned <= 0");
                } else {
                    users = new HashSet<>();
                    debug.send("Collecting builds where a test started failing...");
                    final HashSet<Run<?, ?>> buildsWhereATestStartedFailing = new HashSet<>();
                    for (final TestResult caseResult : testResultAction.getFailedTests()) {
                        final Run<?, ?> runWhereTestStartedFailing = caseResult.getFailedSinceRun();
                        if (runWhereTestStartedFailing != null) {
                            debug.send("  runWhereTestStartedFailing: %d", runWhereTestStartedFailing.getNumber());
                            buildsWhereATestStartedFailing.add(runWhereTestStartedFailing);
                        } else {
                            context.getListener().error("getFailedSinceRun returned null for %s", caseResult.getFullDisplayName());
                        }
                    }
                    // For each build where a test started failing, walk backward looking for build results worse than
                    // UNSTABLE. All of those builds will be used to find suspects.
                    debug.send("Collecting builds with suspects...");
                    final HashSet<Run<?, ?>> buildsWithSuspects = new HashSet<>();
                    for (final Run<?, ?> buildWhereATestStartedFailing : buildsWhereATestStartedFailing) {
                        debug.send("  buildWhereATestStartedFailing: %d", buildWhereATestStartedFailing.getNumber());
                        buildsWithSuspects.add(buildWhereATestStartedFailing);
                        Run<?, ?> previousBuildToCheck = buildWhereATestStartedFailing.getPreviousCompletedBuild();
                        if (previousBuildToCheck != null) {
                            debug.send("    previousBuildToCheck: %d", previousBuildToCheck.getNumber());
                        }
                        while (previousBuildToCheck != null) {
                            if (buildsWithSuspects.contains(previousBuildToCheck)) {
                                // Short-circuit if the build to check has already been checked.
                                debug.send("      already contained in buildsWithSuspects; stopping search");
                                break;
                            }
                            final Result previousResult = previousBuildToCheck.getResult();
                            if (previousResult == null) {
                                debug.send("      previousResult was null");
                            } else {
                                debug.send("      previousResult: %s", previousResult.toString());
                                if (previousResult.isBetterThan(Result.FAILURE)) {
                                    debug.send("      previousResult was better than FAILURE; stopping search");
                                    break;
                                } else {
                                    debug.send("      previousResult was not better than FAILURE; adding to buildsWithSuspects; continuing search");
                                    buildsWithSuspects.add(previousBuildToCheck);
                                    previousBuildToCheck = previousBuildToCheck.getPreviousCompletedBuild();
                                    if (previousBuildToCheck != null) {
                                        debug.send("    previousBuildToCheck: %d", previousBuildToCheck.getNumber());
                                    }
                                }
                            }
                        }
                    }
                    debug.send("Collecting suspects...");
                    users.addAll(RecipientProviderUtilities.getChangeSetAuthors(buildsWithSuspects, debug));
                    users.addAll(RecipientProviderUtilities.getUsersTriggeringTheBuilds(buildsWithSuspects, debug));
                }
            }
        }

        if (users != null) {
            RecipientProviderUtilities.addUsers(users, context, env, to, cc, bcc, debug);
        }
    }

    @Extension
    @Symbol("brokenTestsSuspects")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.FailingTestSuspectsRecipientProvider_DisplayName();
        }
    }
}
