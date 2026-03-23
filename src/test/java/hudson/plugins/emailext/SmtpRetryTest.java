package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketException;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Unit tests for SMTP retry logic, particularly for transient SMTP errors (4xx codes).
 * Since SMTP-specific exception classes are not available at compile time, these tests
 * focus on the message parsing and exception chain traversal logic.
 *
 * @author Akash Manna
 */
class SmtpRetryTest {

    /**
     * Calls the package-private isTransientSmtpError() method using reflection.
     */
    private boolean callIsTransientSmtpError(Exception e) throws Exception {
        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        Method method = ExtendedEmailPublisher.class.getDeclaredMethod("isTransientSmtpError", Exception.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(publisher, e);
    }

    @Test
    @Issue("JENKINS-68518")
    void testIsTransientSmtpErrorWithMessageParsing() throws Exception {
        MessagingException ex421 = new MessagingException("421 Service not available, try again later");
        MessagingException ex450 = new MessagingException("450 Requested mail action not taken: mailbox unavailable");
        MessagingException ex451 = new MessagingException("451 Requested action aborted: error in processing");
        MessagingException ex452 = new MessagingException("452 Insufficient system storage");

        assertTrue(
                callIsTransientSmtpError(ex421),
                "MessagingException with '421' in message should be identified as transient");
        assertTrue(
                callIsTransientSmtpError(ex450),
                "MessagingException with '450' in message should be identified as transient");
        assertTrue(
                callIsTransientSmtpError(ex451),
                "MessagingException with '451' in message should be identified as transient");
        assertTrue(
                callIsTransientSmtpError(ex452),
                "MessagingException with '452' in message should be identified as transient");

        MessagingException ex500 = new MessagingException("500 Syntax error");
        MessagingException ex550 = new MessagingException("550 Requested action not taken");

        assertFalse(
                callIsTransientSmtpError(ex500),
                "MessagingException with '500' in message should NOT be identified as transient");
        assertFalse(
                callIsTransientSmtpError(ex550),
                "MessagingException with '550' in message should NOT be identified as transient");
    }

    @Test
    @Issue("JENKINS-68518")
    void testIsTransientSmtpErrorWithNonSmtpExceptions() throws Exception {
        assertFalse(
                callIsTransientSmtpError(new IOException("Connection error")),
                "IOException should NOT be identified as transient SMTP error");
        assertFalse(
                callIsTransientSmtpError(new MessagingException("Generic messaging error")),
                "MessagingException without SMTP code should NOT be identified as transient");
        assertFalse(
                callIsTransientSmtpError(new SocketException("Connection reset")),
                "SocketException should NOT be identified as transient SMTP error");
    }

    @Test
    @Issue("JENKINS-68518")
    void testIsTransientSmtpErrorInCauseChain() throws Exception {
        MessagingException smtpError = new MessagingException("421 Service not available");
        SendFailedException wrapped = new SendFailedException("Send failed", smtpError);
        MessagingException doubleWrapped = new MessagingException("Multiple errors", wrapped);

        assertTrue(
                callIsTransientSmtpError(wrapped),
                "SMTP 421 error wrapped in SendFailedException should be identified as transient");
        assertTrue(
                callIsTransientSmtpError(doubleWrapped),
                "SMTP 421 error deep in exception chain should be identified as transient");
    }

    @Test
    @Issue("JENKINS-68518")
    void testIsTransientSmtpErrorBoundaryCases() throws Exception {
        MessagingException ex399 = new MessagingException("399 Below 4xx range");
        MessagingException ex400 = new MessagingException("400 First 4xx code");
        MessagingException ex499 = new MessagingException("499 Last 4xx code");
        MessagingException ex500 = new MessagingException("500 First 5xx code");

        assertFalse(callIsTransientSmtpError(ex399), "SMTP 399 should NOT be identified as transient");
        assertTrue(callIsTransientSmtpError(ex400), "SMTP 400 should be identified as transient");
        assertTrue(callIsTransientSmtpError(ex499), "SMTP 499 should be identified as transient");
        assertFalse(callIsTransientSmtpError(ex500), "SMTP 500 should NOT be identified as transient");
    }

    @Test
    @Issue("JENKINS-68518")
    void testIsTransientSmtpErrorWithNextException() throws Exception {
        MessagingException inner = new MessagingException("450 Mailbox unavailable");
        MessagingException outer = new MessagingException("Failed to send");
        outer.setNextException(inner);

        assertTrue(callIsTransientSmtpError(outer), "Should detect transient error in nextException chain");
    }

    @Test
    @Issue("JENKINS-68518")
    void testIsTransientSmtpErrorWithNullCheck() throws Exception {
        assertFalse(callIsTransientSmtpError(null), "Should return false for null exception");
    }
}
