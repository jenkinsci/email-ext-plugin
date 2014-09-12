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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import static org.junit.Assert.assertTrue;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.StreamBuildListener;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.util.StreamTaskListener;

/* package private */ final class TestUtilities {
    private static final String AT_DOMAIN = "@DOMAIN";

    private TestUtilities() {
    }

    public static void checkRecipients(
        final Build<?, ?> build,
        final RecipientProvider provider,
        final String... inAuthors) throws AddressException {
        ExtendedEmailPublisherContext context = new ExtendedEmailPublisherContext(null, build, new Launcher.LocalLauncher(StreamTaskListener.fromStdout()), new StreamBuildListener(System.out, Charset.defaultCharset()));
        EnvVars envVars = new EnvVars();
        Set<InternetAddress> to = new HashSet<InternetAddress>();
        Set<InternetAddress> cc = new HashSet<InternetAddress>();
        Set<InternetAddress> bcc = new HashSet<InternetAddress>();
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

}
