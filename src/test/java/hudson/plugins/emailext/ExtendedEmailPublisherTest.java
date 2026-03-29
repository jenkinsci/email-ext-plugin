package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import jakarta.mail.internet.InternetAddress;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ExtendedEmailPublisherTest {

    @Test
    public void testDuplicateRecipientAcrossToAndCc() throws Exception {

        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();

        ByteArrayOutputStream logStream = new ByteArrayOutputStream();
        TaskListener listener = new StreamTaskListener(logStream);

        Run<?, ?> run = mock(Run.class);

        ExtendedEmailPublisherContext context =
                new ExtendedEmailPublisherContext(publisher, run, null, null, listener);

        InternetAddress addr1 = new InternetAddress("test@example.com");
        InternetAddress addr2 = new InternetAddress("test@example.com");

        Set<InternetAddress> to = new LinkedHashSet<>();
        to.add(addr1);

        Set<InternetAddress> cc = new LinkedHashSet<>();
        cc.add(addr2);

        Set<InternetAddress> bcc = Collections.emptySet();

        publisher.logDuplicateRecipients(context, to, cc, bcc);

        String logs = logStream.toString();

        assertTrue(logs.contains("Duplicate recipient detected"));
        assertTrue(logs.contains("test@example.com"));
        assertTrue(logs.contains("TO"));
        assertTrue(logs.contains("CC"));
    }
}