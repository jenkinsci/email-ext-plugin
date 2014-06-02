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

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.scm.ChangeLogSet;
import hudson.tasks.test.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A recipient provider that assigns ownership of a failing test to the set of developers (including any initiator)
 * that committed changes that first broke the test.
 *
 * InstabilityCommitter
 * Committers or Initiators Causing First Committer
 */
public class FailingTestSuspectsRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public FailingTestSuspectsRecipientProvider() {
    }

    @Override
    public void addRecipients(final ExtendedEmailPublisherContext context, final EnvVars env,
        final Set<InternetAddress> to, final Set<InternetAddress> cc, final Set<InternetAddress> bcc) {

        final class Debug {
            private final ExtendedEmailPublisherDescriptor descriptor
                = Jenkins.getInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }
        final Debug debug = new Debug();

        Set<User> users = null;

        final AbstractBuild<?, ?> currentBuild = context.getBuild();
        if (currentBuild == null) {
            debug.send("currentBuild was null");
        } else {
            final AbstractTestResultAction<?> testResultAction = currentBuild.getTestResultAction();
            if (testResultAction == null) {
                debug.send("testResultAction was null");
            } else {
                if (testResultAction.getFailCount() <= 0) {
                    debug.send("getFailCount() returned <= 0");
                } else {
                    users = new HashSet<User>();
                    debug.send("Collecting builds where a test started failing...");
                    final HashSet<AbstractBuild<?, ?>> buildsWhereATestStartedFailing = new HashSet<AbstractBuild<?, ?>>();
                    for (final TestResult caseResult : testResultAction.getFailedTests()) {
                        final Run<?, ?> runWhereTestStartedFailing = caseResult.getFailedSinceRun();
                        if (runWhereTestStartedFailing instanceof AbstractBuild) {
                            final AbstractBuild<?, ?> buildWhereTestStartedFailing = (AbstractBuild<?, ?>) runWhereTestStartedFailing;
                            debug.send("  buildWhereTestStartedFailing: %d", buildWhereTestStartedFailing.getNumber());
                            buildsWhereATestStartedFailing.add(buildWhereTestStartedFailing);
                        } else {
                            debug.send("  runWhereTestStartedFailing was not an instance of AbstractBuild");
                        }
                    }
                    // For each build where a test started failing, walk backward looking for build results worse than
                    // UNSTABLE. All of those builds will be used to find suspects.
                    debug.send("Collecting builds with suspects...");
                    final HashSet<AbstractBuild<?, ?>> buildsWithSuspects = new HashSet<AbstractBuild<?, ?>>();
                    for (final AbstractBuild<?, ?> buildWhereATestStartedFailing : buildsWhereATestStartedFailing) {
                        debug.send("  buildWhereATestStartedFailing: %d", buildWhereATestStartedFailing.getNumber());
                        buildsWithSuspects.add(buildWhereATestStartedFailing);
                        AbstractBuild<?, ?> previousBuildToCheck = buildWhereATestStartedFailing.getPreviousCompletedBuild();
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
                    for (final AbstractBuild<?, ?> buildWithSuspects : buildsWithSuspects) {
                        debug.send("  buildWithSuspects: %d", buildWithSuspects.getNumber());
                        final ChangeLogSet<?> changeLogSet = buildWithSuspects.getChangeSet();
                        if (changeLogSet == null) {
                            debug.send("    changeLogSet was null");
                        } else {
                            final Set<User> changeAuthors = new HashSet<User>();
                            for (final ChangeLogSet.Entry change : changeLogSet) {
                                final User changeAuthor = change.getAuthor();
                                if (changeAuthors.add(changeAuthor)) {
                                    debug.send("    adding change author: %s", changeAuthor.getFullName());
                                }
                            }
                            users.addAll(changeAuthors);
                        }
                        final User buildRequestor = RequesterRecipientProvider.getUserTriggeringTheBuild(buildWithSuspects);
                        if (buildRequestor != null) {
                            debug.send("    adding build requestor: %s", buildRequestor.getFullName());
                            users.add(buildRequestor);
                        } else {
                            debug.send("    buildRequestor was null");
                        }
                    }
                }
            }
        }

        if (users != null) {
            for (final User user : users) {
                if (EmailRecipientUtils.isExcludedRecipient(user, context.getListener())) {
                    debug.send("User %s is an excluded recipient.", user.getFullName());
                } else {
                    final String userAddress = EmailRecipientUtils.getUserConfiguredEmail(user);
                    if (userAddress != null) {
                        debug.send("Adding %s with address %s", user.getFullName(), userAddress);
                        EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, userAddress, env, context.getListener());
                    } else {
                        context.getListener().getLogger().println("Failed to send e-mail to "
                            + user.getFullName()
                            + " because no e-mail address is known, and no default e-mail domain is configured");
                    }
                }
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        @Override
        public String getDisplayName() {
            return "Suspects Causing Unit Tests to Begin Failing";
        }
    }

}
