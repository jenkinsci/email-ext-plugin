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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.scm.ChangeLogSet;

public final class RecipientProviderUtilities {

    private RecipientProviderUtilities() {
    }

    public interface IDebug {
        void send(final String format, final Object... args);
    }

    public static Set<User> getChangeSetAuthors(final Collection<AbstractBuild<?, ?>> builds, final IDebug debug) {
        debug.send("  Collecting change authors...");
        final Set<User> users = new HashSet<User>();
        for (final AbstractBuild<?, ?> build : builds) {
            debug.send("    build: %d", build.getNumber());
            final ChangeLogSet<?> changeLogSet = build.getChangeSet();
            if (changeLogSet == null) {
                debug.send("      changeLogSet was null");
            } else {
                final Set<User> changeAuthors = new HashSet<User>();
                for (final ChangeLogSet.Entry change : changeLogSet) {
                    final User changeAuthor = change.getAuthor();
                    if (changeAuthors.add(changeAuthor)) {
                        debug.send("      adding author: %s", changeAuthor.getFullName());
                    }
                }
                users.addAll(changeAuthors);
            }
        }
        return users;
    }

    public static Set<User> getUsersTriggeringTheBuilds(final Collection<AbstractBuild<?, ?>> builds, final IDebug debug) {
        debug.send("  Collecting build requestors...");
        final Set<User> users = new HashSet<User>();
        for (final AbstractBuild<?, ?> build : builds) {
            debug.send("    build: %d", build.getNumber());
            final User buildRequestor = RequesterRecipientProvider.getUserTriggeringTheBuild(build);
            if (buildRequestor != null) {
                debug.send("      adding requestor: %s", buildRequestor.getFullName());
                users.add(buildRequestor);
            } else {
                debug.send("      buildRequestor was null");
            }
        }
        return users;
    }

    public static void addUsers(final Set<User> users, final TaskListener listener, final EnvVars env,
        final Set<InternetAddress> to, final Set<InternetAddress> cc, final Set<InternetAddress> bcc, final IDebug debug) {
        for (final User user : users) {
            if (EmailRecipientUtils.isExcludedRecipient(user, listener)) {
                debug.send("User %s is an excluded recipient.", user.getFullName());
            } else {
                final String userAddress = EmailRecipientUtils.getUserConfiguredEmail(user);
                if (userAddress != null) {
                    debug.send("Adding %s with address %s", user.getFullName(), userAddress);
                    EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, userAddress, env, listener);
                } else {
                    listener.getLogger().println("Failed to send e-mail to "
                        + user.getFullName()
                        + " because no e-mail address is known, and no default e-mail domain is configured");
                }
            }
        }
    }

}
