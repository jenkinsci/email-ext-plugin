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
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    ExtendedEmailPublisherDescriptor.class,
    FreeStyleBuild.class,
    Jenkins.class,
    Mailer.class,
    Mailer.DescriptorImpl.class,
    User.class
})
public class FailingTestSuspectsRecipientProviderTest {

    private static final String AT_DOMAIN = "@DOMAIN";

    @Before
    public void before() throws Exception {
        final Jenkins jenkins = PowerMockito.mock(Jenkins.class);
        final ExtendedEmailPublisherDescriptor extendedEmailPublisherDescriptor = PowerMockito.mock(ExtendedEmailPublisherDescriptor.class);
        PowerMockito.when(jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class)).thenReturn(extendedEmailPublisherDescriptor);
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.doReturn(jenkins).when(Jenkins.class, "getInstance");

        final Mailer.DescriptorImpl descriptor = PowerMockito.mock(Mailer.DescriptorImpl.class);
        PowerMockito.when(descriptor.getDefaultSuffix()).thenReturn("DOMAIN");
        PowerMockito.mockStatic(Mailer.class);
        PowerMockito.doReturn(descriptor).when(Mailer.class, "descriptor");
    }

    @Test
    public void testAddRecipients() throws Exception {

        /*
         * Requestor: A
         * No committers.
         * Tests {a,b} fail.
         */
        final FreeStyleBuild build1 = PowerMockito.mock(FreeStyleBuild.class);
        PowerMockito.when(build1.getResult()).thenReturn(Result.UNSTABLE);
        addMockRequestor(build1, "A");
        addMockTestResultAction(build1, build1, build1);
        checkRecipients(build1, "A");

        /*
         * Requestor: (none)
         * Committers {U,V}.
         * Tests {a,b,c} fail.
         */
        final FreeStyleBuild build2 = PowerMockito.mock(FreeStyleBuild.class);
        PowerMockito.when(build2.getPreviousCompletedBuild()).thenReturn(build1);
        PowerMockito.when(build2.getResult()).thenReturn(Result.UNSTABLE);
        addMockChangeSet(build2, "U", "V");
        addMockTestResultAction(build2, build1, build1, build2);
        checkRecipients(build2, "A", "U", "V");

        /**
         * Requestor: (none)
         * Committers {X,V}.
         * Tests {c,d} fail.
         */
        final FreeStyleBuild build3 = PowerMockito.mock(FreeStyleBuild.class);
        PowerMockito.when(build3.getPreviousCompletedBuild()).thenReturn(build2);
        PowerMockito.when(build3.getResult()).thenReturn(Result.UNSTABLE);
        addMockChangeSet(build3, "X", "V");
        addMockTestResultAction(build3, build2, build3);
        checkRecipients(build3, "U", "V", "X");

        /**
         * Requestor: (none)
         * Committers {K}
         * No tests were performed. The build failed.
         */
        final FreeStyleBuild build4 = PowerMockito.mock(FreeStyleBuild.class);
        PowerMockito.when(build4.getPreviousCompletedBuild()).thenReturn(build3);
        PowerMockito.when(build4.getResult()).thenReturn(Result.FAILURE);
        addMockChangeSet(build4, "K");
        checkRecipients(build4);

        /**
         * Requestor: (none)
         * Committers {X,U,V}.
         * No tests were performed. The build failed.
         */
        final FreeStyleBuild build5 = PowerMockito.mock(FreeStyleBuild.class);
        PowerMockito.when(build5.getPreviousCompletedBuild()).thenReturn(build4);
        PowerMockito.when(build5.getResult()).thenReturn(Result.FAILURE);
        addMockChangeSet(build5, "U", "W");
        checkRecipients(build5);

        /**
         * Requestor: A
         * Committers {W}.
         * Tests {a,e (new test)} fail.
         */
        final FreeStyleBuild build6 = PowerMockito.mock(FreeStyleBuild.class);
        PowerMockito.when(build6.getPreviousCompletedBuild()).thenReturn(build5);
        PowerMockito.when(build6.getResult()).thenReturn(Result.UNSTABLE);
        addMockRequestor(build6, "A");
        addMockChangeSet(build6, "W");
        addMockTestResultAction(build6, build6, build6);
        checkRecipients(build6, "A", "K", "U", "W");
    }

    private static void checkRecipients(final Build build, final String... inAuthors) throws AddressException {
        TaskListener listener = StreamBuildListener.fromStdout();
        ExtendedEmailPublisherContext context = new ExtendedEmailPublisherContext(null, build, new Launcher.LocalLauncher(listener), (BuildListener)listener);
        EnvVars envVars = new EnvVars();
        Set<InternetAddress> to = new HashSet<InternetAddress>();
        Set<InternetAddress> cc = new HashSet<InternetAddress>();
        Set<InternetAddress> bcc = new HashSet<InternetAddress>();
        FailingTestSuspectsRecipientProvider provider = new FailingTestSuspectsRecipientProvider();
        provider.addRecipients(context, envVars, to, cc, bcc);
        final List<InternetAddress> authors = new ArrayList<InternetAddress>();
        for (final String author : inAuthors) {
            authors.add(new InternetAddress(author + AT_DOMAIN));
        }
        // All of the authors should have received an email, so the list should be empty.
        authors.removeAll(to);
        assertTrue("Authors not receiving mail: " + authors.toString(), authors.isEmpty());
        assertTrue(cc.isEmpty());
        assertTrue(bcc.isEmpty());
    }

    private static User getMockUser(final String author) {
        final User user = PowerMockito.mock(User.class);
        final Mailer.UserProperty mailProperty = new Mailer.UserProperty(((String) author) + AT_DOMAIN);
        PowerMockito.when(user.getProperty(Mailer.UserProperty.class)).thenReturn(mailProperty);
        return user;
    }

    private static void addMockChangeSet(final AbstractBuild build, final String... inAuthors) {
        PowerMockito.when(build.getChangeSet()).thenReturn(new ChangeLogSet(build) {

            final String[] authors = inAuthors;

            @Override
            public boolean isEmptySet() {
                return authors.length == 0;
            }

            public Iterator iterator() {
                return new TransformIterator(Arrays.asList(authors).iterator(), new Transformer() {
                    @Override
                    public Object transform(final Object inAuthor) {
                        return new ChangeLogSet.Entry() {
                            @Override
                            public String getMsg() {
                                return "COMMIT MESSAGE";
                            }

                            @Override
                            public User getAuthor() {
                                return getMockUser((String) inAuthor);
                            }

                            @Override
                            public Collection<String> getAffectedPaths() {
                                return Collections.EMPTY_SET;
                            }

                            @Override
                            public String getMsgAnnotated() {
                                return getMsg();
                            }

                            @Override
                            public Collection<? extends ChangeLogSet.AffectedFile> getAffectedFiles() {
                                return Collections.EMPTY_SET;
                            }
                        };
                    }

                });
            }
        });
    }

    private static void addMockRequestor(final AbstractBuild build, final String requestor) throws Exception {
        PowerMockito.spy(User.class);
        PowerMockito.doReturn(getMockUser(requestor)).when(User.class, "get", Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyMap());
        final Cause.UserIdCause cause = PowerMockito.mock(Cause.UserIdCause.class);
        PowerMockito.when(cause.getUserId()).thenReturn(requestor);
        PowerMockito.when(build.getCause(Cause.UserIdCause.class)).thenReturn(cause);
    }

    private static void addMockTestResultAction(final AbstractBuild build, final AbstractBuild... failedSinces) {
        final List<CaseResult> failedTests = new LinkedList<CaseResult>();
        for (final AbstractBuild failedSince : failedSinces) {
            final CaseResult caseResult = PowerMockito.mock(CaseResult.class);
            PowerMockito.when(caseResult.getFailedSinceRun()).thenReturn(failedSince);
            failedTests.add(caseResult);
        }
        final TestResultAction testResultAction = PowerMockito.mock(TestResultAction.class);
        PowerMockito.when(testResultAction.getFailedTests()).thenReturn(failedTests);
        PowerMockito.when(testResultAction.getFailCount()).thenReturn(failedTests.size());
        PowerMockito.when(build.getTestResultAction()).thenReturn(testResultAction);
    }
}
