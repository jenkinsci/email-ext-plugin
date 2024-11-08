package hudson.plugins.emailext.plugins.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import hudson.util.StreamTaskListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FailedTestsContentTest {
    private FailedTestsContent failedTestContent;

    private AbstractBuild<?, ?> build;

    private TaskListener listener;

    @Before
    public void setUp() {
        failedTestContent = new FailedTestsContent();
        listener = StreamTaskListener.fromStdout();
        build = mock(AbstractBuild.class);
    }

    @Test
    public void testGetContent_noTestsRanShouldGiveAMeaningfulMessage() throws Exception {
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertEquals("No tests ran.", content);
    }

    /**
     * Verifies that token expansion works for pipeline builds (JENKINS-38519).
     */
    @Test
    public void testGetContent_withWorkspaceAndNoTestsRanShouldGiveAMeaningfulMessage() throws Exception {
        String content =
                failedTestContent.evaluate(build, build.getWorkspace(), listener, FailedTestsContent.MACRO_NAME);

        assertEquals("No tests ran.", content);
    }

    @Test
    public void testGetContent_whenAllTestsPassedShouldGiveMeaningfulMessage() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(0);

        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertEquals("All tests passed", content);
    }

    @Test
    public void testGetContent_whenSomeTestsFailedShouldGiveMeaningfulMessage() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(123);

        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.maxTests = 0;
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertEquals("123 tests failed.", content);
    }

    @Test
    public void testGetContent_withMessage_withStack() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(2);

        List<TestResult> failedTests = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            TestResult result = mock(TestResult.class);
            when(result.isPassed()).thenReturn(false);
            when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
            when(result.getDisplayName()).thenReturn("Test" + i);
            when(result.getErrorDetails()).thenReturn("Error" + i);
            when(result.getErrorStackTrace()).thenReturn("Stack" + i);
            failedTests.add(result);
        }

        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests()).thenReturn(failedTests);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.maxTests = 2;
        failedTestContent.showMessage = true;
        failedTestContent.showStack = true;
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        for (int i = 0; i < 2; i++) {
            assertTrue(content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i));
            assertTrue(content.contains("Error Message:\nError" + i));
            assertTrue(content.contains("Stack Trace:\nStack" + i));
        }
    }

    @Test
    public void testGetContent_noMessage_withStack() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(2);

        List<TestResult> failedTests = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            TestResult result = mock(TestResult.class);
            when(result.isPassed()).thenReturn(false);
            when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
            when(result.getDisplayName()).thenReturn("Test" + i);
            when(result.getErrorDetails()).thenReturn("Error" + i);
            when(result.getErrorStackTrace()).thenReturn("Stack" + i);
            failedTests.add(result);
        }
        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests()).thenReturn(failedTests);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.maxTests = 2;
        failedTestContent.showMessage = false;
        failedTestContent.showStack = true;
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        for (int i = 0; i < 2; i++) {
            assertTrue(content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i));
            assertFalse(content.contains("Error Message:\nError" + i));
            assertTrue(content.contains("Stack Trace:\nStack" + i));
        }
    }

    @Test
    public void testGetContent_withMessage_noStack() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(2);

        List<TestResult> failedTests = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            TestResult result = mock(TestResult.class);
            when(result.isPassed()).thenReturn(false);
            when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
            when(result.getDisplayName()).thenReturn("Test" + i);
            when(result.getErrorDetails()).thenReturn("Error" + i);
            when(result.getErrorStackTrace()).thenReturn("Stack" + i);
            failedTests.add(result);
        }
        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests()).thenReturn(failedTests);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.maxTests = 2;
        failedTestContent.showMessage = true;
        failedTestContent.showStack = false;
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        for (int i = 0; i < 2; i++) {
            assertTrue(content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i));
            assertTrue(content.contains("Error Message:\nError" + i));
            assertFalse(content.contains("Stack Trace:\nStack" + i));
        }
    }

    @Test
    public void testGetContent_noMessage_noStack() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(2);

        List<TestResult> failedTests = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            TestResult result = mock(TestResult.class);
            when(result.isPassed()).thenReturn(false);
            when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
            when(result.getDisplayName()).thenReturn("Test" + i);
            when(result.getErrorDetails()).thenReturn("Error" + i);
            when(result.getErrorStackTrace()).thenReturn("Stack" + i);
            failedTests.add(result);
        }
        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests()).thenReturn(failedTests);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.maxTests = 2;
        failedTestContent.showMessage = false;
        failedTestContent.showStack = false;
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        for (int i = 0; i < 2; i++) {
            assertTrue(content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i));
            assertFalse(content.contains("Error Message:\nError" + i));
            assertFalse(content.contains("Stack Trace:\nStack" + i));
        }
    }

    @Test
    public void testGetContent_whenContentLargerThanMaxLengthShouldTruncate() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(5);

        List<TestResult> failedTests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TestResult result = mock(TestResult.class);
            when(result.isPassed()).thenReturn(false);
            when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
            when(result.getDisplayName()).thenReturn("Test" + i);
            when(result.getErrorDetails()).thenReturn(StringUtils.leftPad("", 3 * 1024, 'z'));
            when(result.getErrorStackTrace()).thenReturn(StringUtils.leftPad("", 200, 'e'));
            failedTests.add(result);
        }

        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests()).thenReturn(failedTests);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.maxLength = 10;
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);
        assertTrue(content.length() < (3 * 1024 * 5));

        failedTestContent = new FailedTestsContent();
        failedTestContent.showStack = true;
        content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);
        assertTrue(content.length() >= (3 * 1024 * 5));
    }

    @Test
    public void testGetContent_withMessage_withStack_htmlEscaped() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(1);

        TestResult result = mock(TestResult.class);
        when(result.isPassed()).thenReturn(false);
        when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
        when(result.getDisplayName()).thenReturn("Test");
        when(result.getErrorDetails()).thenReturn("expected:<ABORTED> but was:<COMPLETED> ");
        when(result.getErrorStackTrace()).thenReturn("at org.nexusformat.NexusFile.<clinit>(NexusFile.java:99)");

        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests())
                .thenReturn(Collections.singletonList(result));
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.showMessage = true;
        failedTestContent.showStack = true;
        failedTestContent.escapeHtml = true;
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertEquals(
                content,
                "1 tests failed.<br/>" + "FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test<br/><br/>"
                        + "Error Message:<br/>"
                        + "expected:&lt;ABORTED&gt; but was:&lt;COMPLETED&gt; <br/><br/>"
                        + "Stack Trace:<br/>"
                        + "at org.nexusformat.NexusFile.&lt;clinit&gt;(NexusFile.java:99)<br/><br/>");
    }

    @Test
    public void testGetContent_withMessage_withStack_outputYaml() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(1);
        String testStackTrace =
                """
                javax.servlet.ServletException: Something bad happened
                    at com.example.myproject.OpenSessionInViewFilter.doFilter(OpenSessionInViewFilter.java:60)
                    at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1157)
                    at com.example.myproject.ExceptionHandlerFilter.doFilter(ExceptionHandlerFilter.java:28)
                    at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1157)
                    at com.example.myproject.OutputBufferFilter.doFilter(OutputBufferFilter.java:33)
                    at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1157)
                    at org.mortbay.jetty.servlet.ServletHandler.handle(ServletHandler.java:388)
                    at org.mortbay.jetty.security.SecurityHandler.handle(SecurityHandler.java:216)
                    at org.mortbay.jetty.servlet.SessionHandler.handle(SessionHandler.java:182)
                    at org.mortbay.jetty.handler.ContextHandler.handle(ContextHandler.java:765)
                    at org.mortbay.jetty.webapp.WebAppContext.handle(WebAppContext.java:418)
                    at org.mortbay.jetty.handler.HandlerWrapper.handle(HandlerWrapper.java:152)
                    at org.mortbay.jetty.Server.handle(Server.java:326)
                    at org.mortbay.jetty.HttpConnection.handleRequest(HttpConnection.java:542)
                    at org.mortbay.jetty.HttpConnection$RequestHandler.content(HttpConnection.java:943)
                    at org.mortbay.jetty.HttpParser.parseNext(HttpParser.java:756)
                    at org.mortbay.jetty.HttpParser.parseAvailable(HttpParser.java:218)
                    at org.mortbay.jetty.HttpConnection.handle(HttpConnection.java:404)
                    at org.mortbay.jetty.bio.SocketConnector$Connection.run(SocketConnector.java:228)
                    at org.mortbay.thread.QueuedThreadPool$PoolThread.run(QueuedThreadPool.java:582)
                Caused by: com.example.myproject.MyProjectServletException
                    at com.example.myproject.MyServlet.doPost(MyServlet.java:169)
                    at javax.servlet.http.HttpServlet.service(HttpServlet.java:727)
                    at javax.servlet.http.HttpServlet.service(HttpServlet.java:820)
                    at org.mortbay.jetty.servlet.ServletHolder.handle(ServletHolder.java:511)
                    at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1166)
                    at com.example.myproject.OpenSessionInViewFilter.doFilter(OpenSessionInViewFilter.java:30)
                    ... 27 more
                Caused by: org.hibernate.exception.ConstraintViolationException: could not insert: [com.example.myproject.MyEntity]
                    at org.hibernate.exception.SQLStateConverter.convert(SQLStateConverter.java:96)
                    at org.hibernate.exception.JDBCExceptionHelper.convert(JDBCExceptionHelper.java:66)
                    at org.hibernate.id.insert.AbstractSelectingDelegate.performInsert(AbstractSelectingDelegate.java:64)
                    at org.hibernate.persister.entity.AbstractEntityPersister.insert(AbstractEntityPersister.java:2329)
                    at org.hibernate.persister.entity.AbstractEntityPersister.insert(AbstractEntityPersister.java:2822)
                    at org.hibernate.action.EntityIdentityInsertAction.execute(EntityIdentityInsertAction.java:71)
                    at org.hibernate.engine.ActionQueue.execute(ActionQueue.java:268)
                    at org.hibernate.event.def.AbstractSaveEventListener.performSaveOrReplicate(AbstractSaveEventListener.java:321)
                    at org.hibernate.event.def.AbstractSaveEventListener.performSave(AbstractSaveEventListener.java:204)
                    at org.hibernate.event.def.AbstractSaveEventListener.saveWithGeneratedId(AbstractSaveEventListener.java:130)
                    at org.hibernate.event.def.DefaultSaveOrUpdateEventListener.saveWithGeneratedOrRequestedId(DefaultSaveOrUpdateEventListener.java:210)
                    at org.hibernate.event.def.DefaultSaveEventListener.saveWithGeneratedOrRequestedId(DefaultSaveEventListener.java:56)
                    at org.hibernate.event.def.DefaultSaveOrUpdateEventListener.entityIsTransient(DefaultSaveOrUpdateEventListener.java:195)
                    at org.hibernate.event.def.DefaultSaveEventListener.performSaveOrUpdate(DefaultSaveEventListener.java:50)
                    at org.hibernate.event.def.DefaultSaveOrUpdateEventListener.onSaveOrUpdate(DefaultSaveOrUpdateEventListener.java:93)
                    at org.hibernate.impl.SessionImpl.fireSave(SessionImpl.java:705)
                    at org.hibernate.impl.SessionImpl.save(SessionImpl.java:693)
                    at org.hibernate.impl.SessionImpl.save(SessionImpl.java:689)
                    at sun.reflect.GeneratedMethodAccessor5.invoke(Unknown Source)
                    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
                    at java.lang.reflect.Method.invoke(Method.java:597)
                    at org.hibernate.context.ThreadLocalSessionContext$TransactionProtectionWrapper.invoke(ThreadLocalSessionContext.java:344)
                    at $Proxy19.save(Unknown Source)
                    at com.example.myproject.MyEntityService.save(MyEntityService.java:59) <-- relevant call (see notes below)
                    at com.example.myproject.MyServlet.doPost(MyServlet.java:164)
                    ... 32 more
                Caused by: java.sql.SQLException: Violation of unique constraint MY_ENTITY_UK_1: duplicate value(s) for column(s) MY_COLUMN in statement [...]
                    at org.hsqldb.jdbc.Util.throwError(Unknown Source)
                    at org.hsqldb.jdbc.jdbcPreparedStatement.executeUpdate(Unknown Source)
                    at com.mchange.v2.c3p0.impl.NewProxyPreparedStatement.executeUpdate(NewProxyPreparedStatement.java:105)
                    at org.hibernate.id.insert.AbstractSelectingDelegate.performInsert(AbstractSelectingDelegate.java:57)
                    ... 54 more""";

        TestResult result = mock(TestResult.class);
        when(result.isPassed()).thenReturn(false);
        when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
        when(result.getDisplayName()).thenReturn("Test");
        when(result.getErrorDetails()).thenReturn("expected:<ABORTED> but was:<COMPLETED> ");
        when(result.getErrorStackTrace()).thenReturn(testStackTrace);

        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests())
                .thenReturn(Collections.singletonList(result));
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.showMessage = true;
        failedTestContent.showStack = true;
        failedTestContent.outputFormat = "yaml";
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);
        assertEquals(
                content,
                "summary: \"1 tests failed.\"\n" + "tests:\n"
                        + "- name: \"hudson.plugins.emailext.ExtendedEmailPublisherTest.Test\"\n"
                        + "  status: \"FAILED\"\n"
                        + "  errorMessage: \"expected:<ABORTED> but was:<COMPLETED> \"\n"
                        + "  stackTrace: |-\n"
                        + testStackTrace.replaceAll("(?m)^", "    ") + "\n" + "otherFailedTests: false\n"
                        + "truncatedOutput: false\n");
    }

    @Test
    public void testGetContent_withMessage_withStack_specificTestSuite() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);

        List<TestResult> failedTests = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            TestResult result = mock(TestResult.class);
            when(result.isPassed()).thenReturn(false);
            when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
            when(result.getDisplayName()).thenReturn("Test" + i);
            when(result.getErrorDetails()).thenReturn("Error" + i);
            when(result.getErrorStackTrace()).thenReturn("Stack" + i);
            failedTests.add(result);
        }

        for (int i = 0; i < 2; i++) {
            TestResult result = mock(TestResult.class);
            when(result.isPassed()).thenReturn(false);
            when(result.getFullName()).thenReturn("hudson.plugins.emailext.OtherPackageTest");
            when(result.getDisplayName()).thenReturn("Test" + i);
            when(result.getErrorDetails()).thenReturn("Error" + i);
            when(result.getErrorStackTrace()).thenReturn("Stack" + i);
            failedTests.add(result);
        }

        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests()).thenReturn(failedTests);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.maxTests = 4;
        failedTestContent.showMessage = true;
        failedTestContent.showStack = true;
        failedTestContent.testNamePattern = ".*ExtendedEmailPublisherTest.*";
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertTrue(content.contains(2 + " tests failed"));
        for (int i = 0; i < 2; i++) {
            assertTrue(content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i));
            assertTrue(content.contains("Error Message:\nError" + i));
            assertTrue(content.contains("Stack Trace:\nStack" + i));
        }
        for (int i = 0; i < 2; i++) {
            assertFalse(content.contains("FAILED:  hudson.plugins.emailext.OtherPackageTest.Test" + i));
        }
    }

    @Test
    public void testGetContent_withMessage_withStack_allTestSuites() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);

        List<TestResult> failedTests = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            TestResult result = mock(TestResult.class);
            when(result.isPassed()).thenReturn(false);
            when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
            when(result.getDisplayName()).thenReturn("Test" + i);
            when(result.getErrorDetails()).thenReturn("Error" + i);
            when(result.getErrorStackTrace()).thenReturn("Stack" + i);
            failedTests.add(result);
        }

        for (int i = 0; i < 2; i++) {
            TestResult result = mock(TestResult.class);
            when(result.isPassed()).thenReturn(false);
            when(result.getFullName()).thenReturn("hudson.plugins.emailext.OtherPackageTest");
            when(result.getDisplayName()).thenReturn("Test" + i);
            when(result.getErrorDetails()).thenReturn("Error" + i);
            when(result.getErrorStackTrace()).thenReturn("Stack" + i);
            failedTests.add(result);
        }

        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests()).thenReturn(failedTests);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.maxTests = 4;
        failedTestContent.showMessage = true;
        failedTestContent.showStack = true;
        failedTestContent.testNamePattern = ".*";
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertTrue(content.contains(4 + " tests failed"));
        for (int i = 0; i < 2; i++) {
            assertTrue(content.contains("FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test" + i));
            assertTrue(content.contains("Error Message:\nError" + i));
            assertTrue(content.contains("Stack Trace:\nStack" + i));
        }
        for (int i = 0; i < 2; i++) {
            assertTrue(content.contains("FAILED:  hudson.plugins.emailext.OtherPackageTest.Test" + i));
        }
    }
}
