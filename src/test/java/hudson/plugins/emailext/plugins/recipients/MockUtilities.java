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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections.iterators.TransformIterator;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/* package private */ final class MockUtilities {
    private static final String AT_DOMAIN = "@DOMAIN";

    private MockUtilities() {
    }

    public static User getUser(final String author) {
        final User user = Mockito.mock(User.class);
        final Mailer.UserProperty mailProperty = new Mailer.UserProperty(author + AT_DOMAIN);
        Mockito.when(user.getProperty(Mailer.UserProperty.class)).thenReturn(mailProperty);
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
            return new TransformIterator(Arrays.asList(authors).iterator(), inAuthor -> new Entry() {
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
                public Collection<? extends AffectedFile> getAffectedFiles() {
                    return Collections.emptySet();
                }
            });
        }

    }

    public static ChangeLogSet<ChangeLogSet.Entry> makeChangeSet(final Run<?, ?> build, final String... inAuthors) {
        return new MockUtilitiesChangeSet(build, inAuthors);
    }

    public static void addChangeSet(final WorkflowRun build, final String... inAuthors) {
        ChangeLogSet<ChangeLogSet.Entry> changeSet = makeChangeSet(build, inAuthors);
        Mockito.when(build.getChangeSets()).thenReturn(Collections.singletonList(changeSet));
    }

    public static void addChangeSet(final AbstractBuild<?, ?> build, final String... inAuthors) {
        ChangeLogSet<ChangeLogSet.Entry> changeSet = makeChangeSet(build, inAuthors);
        Mockito.doReturn(changeSet).when(build).getChangeSet();
    }

    public static void addRequestor(final MockedStatic<User> mockedUser, final AbstractBuild<?, ?> build, final String requestor) {
        mockedUser.when(() -> User.get(Mockito.anyString(), Mockito.anyBoolean(), Mockito.any())).then((Answer<User>) invocation -> {
            Object[] args = invocation.getArguments();
            return getUser((String) args[0]);
        });
        final Cause.UserIdCause cause = Mockito.mock(Cause.UserIdCause.class);
        Mockito.when(cause.getUserId()).thenReturn(requestor);
        Mockito.doReturn(cause).when(build).getCause(Cause.UserIdCause.class);
    }

    public static void addTestResultAction(final AbstractBuild<?, ?> build, final AbstractBuild<?, ?>... failedSinces) {
        final List<CaseResult> failedTests = new LinkedList<>();
        for (final AbstractBuild failedSince : failedSinces) {
            final CaseResult caseResult = Mockito.mock(CaseResult.class);
            Mockito.when(caseResult.getFailedSinceRun()).thenReturn(failedSince);
            failedTests.add(caseResult);
        }
        final TestResultAction testResultAction = Mockito.mock(TestResultAction.class);
        Mockito.when(testResultAction.getFailedTests()).thenReturn(failedTests);
        Mockito.when(testResultAction.getFailCount()).thenReturn(failedTests.size());
        Mockito.doReturn(testResultAction).when(build).getAction(AbstractTestResultAction.class);
    }

}
