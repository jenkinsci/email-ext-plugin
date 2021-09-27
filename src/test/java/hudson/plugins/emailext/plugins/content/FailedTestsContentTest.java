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
    public void testGetContent_noTestsRanShouldGiveAMeaningfulMessage()
            throws Exception {
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertEquals("No tests ran.", content);
    }

    /**
     * Verifies that token expansion works for pipeline builds (JENKINS-38519).
     */
    @Test
    public void testGetContent_withWorkspaceAndNoTestsRanShouldGiveAMeaningfulMessage()
            throws Exception {
        String content = failedTestContent.evaluate(build, build.getWorkspace(), listener, FailedTestsContent.MACRO_NAME);

        assertEquals("No tests ran.", content);
    }

    @Test
    public void testGetContent_whenAllTestsPassedShouldGiveMeaningfulMessage()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(0);

        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertEquals("All tests passed", content);
    }

    @Test
    public void testGetContent_whenSomeTestsFailedShouldGiveMeaningfulMessage()
            throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(123);

        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.maxTests = 0;
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertEquals("123 tests failed.", content);
    }

    @Test
    public void testGetContent_withMessage_withStack()
            throws Exception {
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
    public void testGetContent_noMessage_withStack()
            throws Exception {
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
    public void testGetContent_withMessage_noStack()
            throws Exception {
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
    public void testGetContent_noMessage_noStack()
            throws Exception {
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
    public void testGetContent_whenContentLargerThanMaxLengthShouldTruncate()
            throws Exception {
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

        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests()).thenReturn(Collections.singletonList(result));
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.showMessage = true;
        failedTestContent.showStack = true;
        failedTestContent.escapeHtml = true;
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);

        assertEquals(content, "1 tests failed.<br/>" +
                "FAILED:  hudson.plugins.emailext.ExtendedEmailPublisherTest.Test<br/><br/>" +
                "Error Message:<br/>" +
                "expected:&lt;ABORTED&gt; but was:&lt;COMPLETED&gt; <br/><br/>" +
                "Stack Trace:<br/>" +
                "at org.nexusformat.NexusFile.&lt;clinit&gt;(NexusFile.java:99)<br/><br/>");
    }

    @Test
    public void testGetContent_withMessage_withStack_outputYaml() throws Exception {
        AbstractTestResultAction<?> testResults = mock(AbstractTestResultAction.class);
        when(testResults.getFailCount()).thenReturn(1);
        String testStackTrace = "javax.servlet.ServletException: Something bad happened\n" +
                "    at com.example.myproject.OpenSessionInViewFilter.doFilter(OpenSessionInViewFilter.java:60)\n" +
                "    at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1157)\n" +
                "    at com.example.myproject.ExceptionHandlerFilter.doFilter(ExceptionHandlerFilter.java:28)\n" +
                "    at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1157)\n" +
                "    at com.example.myproject.OutputBufferFilter.doFilter(OutputBufferFilter.java:33)\n" +
                "    at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1157)\n" +
                "    at org.mortbay.jetty.servlet.ServletHandler.handle(ServletHandler.java:388)\n" +
                "    at org.mortbay.jetty.security.SecurityHandler.handle(SecurityHandler.java:216)\n" +
                "    at org.mortbay.jetty.servlet.SessionHandler.handle(SessionHandler.java:182)\n" +
                "    at org.mortbay.jetty.handler.ContextHandler.handle(ContextHandler.java:765)\n" +
                "    at org.mortbay.jetty.webapp.WebAppContext.handle(WebAppContext.java:418)\n" +
                "    at org.mortbay.jetty.handler.HandlerWrapper.handle(HandlerWrapper.java:152)\n" +
                "    at org.mortbay.jetty.Server.handle(Server.java:326)\n" +
                "    at org.mortbay.jetty.HttpConnection.handleRequest(HttpConnection.java:542)\n" +
                "    at org.mortbay.jetty.HttpConnection$RequestHandler.content(HttpConnection.java:943)\n" +
                "    at org.mortbay.jetty.HttpParser.parseNext(HttpParser.java:756)\n" +
                "    at org.mortbay.jetty.HttpParser.parseAvailable(HttpParser.java:218)\n" +
                "    at org.mortbay.jetty.HttpConnection.handle(HttpConnection.java:404)\n" +
                "    at org.mortbay.jetty.bio.SocketConnector$Connection.run(SocketConnector.java:228)\n" +
                "    at org.mortbay.thread.QueuedThreadPool$PoolThread.run(QueuedThreadPool.java:582)\n" +
                "Caused by: com.example.myproject.MyProjectServletException\n" +
                "    at com.example.myproject.MyServlet.doPost(MyServlet.java:169)\n" +
                "    at javax.servlet.http.HttpServlet.service(HttpServlet.java:727)\n" +
                "    at javax.servlet.http.HttpServlet.service(HttpServlet.java:820)\n" +
                "    at org.mortbay.jetty.servlet.ServletHolder.handle(ServletHolder.java:511)\n" +
                "    at org.mortbay.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1166)\n" +
                "    at com.example.myproject.OpenSessionInViewFilter.doFilter(OpenSessionInViewFilter.java:30)\n" +
                "    ... 27 more\n" +
                "Caused by: org.hibernate.exception.ConstraintViolationException: could not insert: [com.example.myproject.MyEntity]\n" +
                "    at org.hibernate.exception.SQLStateConverter.convert(SQLStateConverter.java:96)\n" +
                "    at org.hibernate.exception.JDBCExceptionHelper.convert(JDBCExceptionHelper.java:66)\n" +
                "    at org.hibernate.id.insert.AbstractSelectingDelegate.performInsert(AbstractSelectingDelegate.java:64)\n" +
                "    at org.hibernate.persister.entity.AbstractEntityPersister.insert(AbstractEntityPersister.java:2329)\n" +
                "    at org.hibernate.persister.entity.AbstractEntityPersister.insert(AbstractEntityPersister.java:2822)\n" +
                "    at org.hibernate.action.EntityIdentityInsertAction.execute(EntityIdentityInsertAction.java:71)\n" +
                "    at org.hibernate.engine.ActionQueue.execute(ActionQueue.java:268)\n" +
                "    at org.hibernate.event.def.AbstractSaveEventListener.performSaveOrReplicate(AbstractSaveEventListener.java:321)\n" +
                "    at org.hibernate.event.def.AbstractSaveEventListener.performSave(AbstractSaveEventListener.java:204)\n" +
                "    at org.hibernate.event.def.AbstractSaveEventListener.saveWithGeneratedId(AbstractSaveEventListener.java:130)\n" +
                "    at org.hibernate.event.def.DefaultSaveOrUpdateEventListener.saveWithGeneratedOrRequestedId(DefaultSaveOrUpdateEventListener.java:210)\n" +
                "    at org.hibernate.event.def.DefaultSaveEventListener.saveWithGeneratedOrRequestedId(DefaultSaveEventListener.java:56)\n" +
                "    at org.hibernate.event.def.DefaultSaveOrUpdateEventListener.entityIsTransient(DefaultSaveOrUpdateEventListener.java:195)\n" +
                "    at org.hibernate.event.def.DefaultSaveEventListener.performSaveOrUpdate(DefaultSaveEventListener.java:50)\n" +
                "    at org.hibernate.event.def.DefaultSaveOrUpdateEventListener.onSaveOrUpdate(DefaultSaveOrUpdateEventListener.java:93)\n" +
                "    at org.hibernate.impl.SessionImpl.fireSave(SessionImpl.java:705)\n" +
                "    at org.hibernate.impl.SessionImpl.save(SessionImpl.java:693)\n" +
                "    at org.hibernate.impl.SessionImpl.save(SessionImpl.java:689)\n" +
                "    at sun.reflect.GeneratedMethodAccessor5.invoke(Unknown Source)\n" +
                "    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n" +
                "    at java.lang.reflect.Method.invoke(Method.java:597)\n" +
                "    at org.hibernate.context.ThreadLocalSessionContext$TransactionProtectionWrapper.invoke(ThreadLocalSessionContext.java:344)\n" +
                "    at $Proxy19.save(Unknown Source)\n" +
                "    at com.example.myproject.MyEntityService.save(MyEntityService.java:59) <-- relevant call (see notes below)\n" +
                "    at com.example.myproject.MyServlet.doPost(MyServlet.java:164)\n" +
                "    ... 32 more\n" +
                "Caused by: java.sql.SQLException: Violation of unique constraint MY_ENTITY_UK_1: duplicate value(s) for column(s) MY_COLUMN in statement [...]\n" +
                "    at org.hsqldb.jdbc.Util.throwError(Unknown Source)\n" +
                "    at org.hsqldb.jdbc.jdbcPreparedStatement.executeUpdate(Unknown Source)\n" +
                "    at com.mchange.v2.c3p0.impl.NewProxyPreparedStatement.executeUpdate(NewProxyPreparedStatement.java:105)\n" +
                "    at org.hibernate.id.insert.AbstractSelectingDelegate.performInsert(AbstractSelectingDelegate.java:57)\n" +
                "    ... 54 more";

        TestResult result = mock(TestResult.class);
        when(result.isPassed()).thenReturn(false);
        when(result.getFullName()).thenReturn("hudson.plugins.emailext.ExtendedEmailPublisherTest");
        when(result.getDisplayName()).thenReturn("Test");
        when(result.getErrorDetails()).thenReturn("expected:<ABORTED> but was:<COMPLETED> ");
        when(result.getErrorStackTrace()).thenReturn(testStackTrace);

        Mockito.<List<? extends TestResult>>when(testResults.getFailedTests()).thenReturn(Collections.singletonList(result));
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(testResults);

        failedTestContent.showMessage = true;
        failedTestContent.showStack = true;
        failedTestContent.outputFormat = "yaml";
        String content = failedTestContent.evaluate(build, listener, FailedTestsContent.MACRO_NAME);
        assertEquals(content, "summary: \"1 tests failed.\"\n" +
                "tests:\n" +
                "- name: \"hudson.plugins.emailext.ExtendedEmailPublisherTest.Test\"\n" +
                "  status: \"FAILED\"\n" +
                "  errorMessage: \"expected:<ABORTED> but was:<COMPLETED> \"\n" +
                "  stackTrace: |-\n" + testStackTrace.replaceAll("(?m)^", "    ") + "\n" +
                "otherFailedTests: false\n" +
                "truncatedOutput: false\n");
    }

    @Test
    public void testGetContent_withMessage_withStack_specificTestSuite()
            throws Exception {
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
    public void testGetContent_withMessage_withStack_allTestSuites()
            throws Exception {
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