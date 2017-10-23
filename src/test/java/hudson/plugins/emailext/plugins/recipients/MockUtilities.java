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

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/* package private */ final class MockUtilities {
    private static final String AT_DOMAIN = "@DOMAIN";

    private MockUtilities() {
    }

    public static User getUser(final String author) {
        final User user = PowerMockito.mock(User.class);
        final Mailer.UserProperty mailProperty = new Mailer.UserProperty(author + AT_DOMAIN);
        PowerMockito.when(user.getProperty(Mailer.UserProperty.class)).thenReturn(mailProperty);
        return user;
    }

    public static class MockUtilitiesChangeSet extends ChangeLogSet<ChangeLogSet.Entry> {
        final String[] authors;
        final Run<?, ?> build;

        public MockUtilitiesChangeSet(Run<?, ?> build, final String... authors) {
            super(build, null);
            this.build = build;
            this.authors = authors;
        }

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
                            return getUser((String) inAuthor);
                        }

                        @Override
                        public Collection<String> getAffectedPaths() {
                            return Collections.emptySet();
                        }

                        @Override
                        public String getMsgAnnotated() {
                            return getMsg();
                        }

                        @Override
                        public Collection<? extends ChangeLogSet.AffectedFile> getAffectedFiles() {
                            return Collections.emptySet();
                        }
                    };
                }

            });
        }

    }

    public static ChangeLogSet<ChangeLogSet.Entry> makeChangeSet(final Run<?, ?> build, final String... inAuthors) {
        return new MockUtilitiesChangeSet(build, inAuthors);
    }

    public static void addChangeSet(final WorkflowRun build, final String... inAuthors) {
        ChangeLogSet<ChangeLogSet.Entry> changeSet = makeChangeSet(build, inAuthors);
        PowerMockito.when(build.getChangeSets()).thenReturn(Collections.<ChangeLogSet<? extends ChangeLogSet.Entry>>singletonList(changeSet));
    }

    public static void addChangeSet(final AbstractBuild<?, ?> build, final String... inAuthors) {
        ChangeLogSet<ChangeLogSet.Entry> changeSet = makeChangeSet(build, inAuthors);
        PowerMockito.doReturn(changeSet).when(build).getChangeSet();
    }

    public static void addRequestor(final AbstractBuild<?, ?> build, final String requestor) throws Exception {
        PowerMockito.spy(User.class);
        PowerMockito.doAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return getUser((String) args[0]);
            }
        }).when(User.class, "get", Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyMap());
        final Cause.UserIdCause cause = PowerMockito.mock(Cause.UserIdCause.class);
        PowerMockito.when(cause.getUserId()).thenReturn(requestor);
        PowerMockito.doReturn(cause).when(build).getCause(Cause.UserIdCause.class);
    }

    public static void addTestResultAction(final AbstractBuild<?, ?> build, final AbstractBuild<?, ?>... failedSinces) {
        final List<CaseResult> failedTests = new LinkedList<CaseResult>();
        for (final AbstractBuild failedSince : failedSinces) {
            final CaseResult caseResult = PowerMockito.mock(CaseResult.class);
            PowerMockito.when(caseResult.getFailedSinceRun()).thenReturn(failedSince);
            failedTests.add(caseResult);
        }
        final TestResultAction testResultAction = PowerMockito.mock(TestResultAction.class);
        PowerMockito.when(testResultAction.getFailedTests()).thenReturn(failedTests);
        PowerMockito.when(testResultAction.getFailCount()).thenReturn(failedTests.size());
        PowerMockito.doReturn(testResultAction).when(build).getAction(AbstractTestResultAction.class);
    }

}
