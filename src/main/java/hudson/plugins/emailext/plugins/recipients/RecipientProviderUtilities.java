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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.scm.ChangeLogSet;
import hudson.tasks.MailSender;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.mail.internet.InternetAddress;
import jenkins.model.Jenkins;
import jenkins.scm.RunWithSCM;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public final class RecipientProviderUtilities {
    private static final Logger LOGGER = Logger.getLogger(RecipientProviderUtilities.class.getName());

    private RecipientProviderUtilities() {
    }

    public interface IDebug {
        void send(final String format, final Object... args);
    }

    public static Set<User> getChangeSetAuthors(final Collection<Run<?, ?>> runs, final IDebug debug) {
        debug.send("  Collecting change authors...");
        final Set<User> users = new HashSet<>();
        for (final Run<?, ?> run : runs) {
            debug.send("    build: %d", run.getNumber());
            if (run instanceof RunWithSCM) {
                List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = ((RunWithSCM<?, ?>) run).getChangeSets();
                for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
                    addChangeSetUsers(changeSet, users, debug);
                }
            }
        }
        return users;
    }

    private static void addChangeSetUsers(ChangeLogSet<?> changeLogSet, Set<User> users, IDebug debug) {
        final Set<User> changeAuthors = new HashSet<>();
        for (final ChangeLogSet.Entry change : changeLogSet) {
            final User changeAuthor = change.getAuthor();
            if (changeAuthors.add(changeAuthor)) {
                debug.send("      adding author: %s", changeAuthor.getFullName());
            }
        }
        users.addAll(changeAuthors);
    }

    public static Set<User> getUsersTriggeringTheBuilds(final Collection<Run<?, ?>> runs, final IDebug debug) {
        debug.send("  Collecting build requestors...");
        final Set<User> users = new HashSet<>();
        for (final Run<?, ?> run : runs) {
            debug.send("    build: %d", run.getNumber());
            final User buildRequestor = getUserTriggeringTheBuild(run);
            if (buildRequestor != null) {
                debug.send("      adding requestor: %s", buildRequestor.getFullName());
                users.add(buildRequestor);
            } else {
                debug.send("      buildRequestor was null");
            }
        }
        return users;
    }

    private static User getByUserIdCause(Run<?, ?> run) {
        try {
            Cause.UserIdCause cause = run.getCause(Cause.UserIdCause.class);
            if (cause != null) {
                String id = cause.getUserId();
                return User.get(id, false, Collections.emptyMap());
            }

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("deprecated")
    private static User getByLegacyUserCause(Run<?, ?> run) {
        try {
            Cause.UserCause userCause = run.getCause(Cause.UserCause.class);
            // userCause.getUserName() returns displayName which may be different from authentication name
            // Therefore use reflection to access the real authenticationName
            if (userCause != null) {
                Field authenticationName = Cause.UserCause.class.getDeclaredField("authenticationName");
                authenticationName.setAccessible(true);
                String name = (String) authenticationName.get(userCause);
                return User.get(name, false, Collections.emptyMap());
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        return null;
    }

    public static User getUserTriggeringTheBuild(final Run<?, ?> run) {
        User user = getByUserIdCause(run);
        if (user == null) {
            user = getByLegacyUserCause(run);
        }
        return user;
    }

    @Deprecated
    public static void addUsers(final Set<User> users, final TaskListener listener, final EnvVars env,
        final Set<InternetAddress> to, final Set<InternetAddress> cc, final Set<InternetAddress> bcc, final IDebug debug) {
        addUsers(users, listener, null, env, to, cc, bcc, debug);
    }

    public static void addUsers(final Set<User> users, final ExtendedEmailPublisherContext context, final EnvVars env,
        final Set<InternetAddress> to, final Set<InternetAddress> cc, final Set<InternetAddress> bcc, final IDebug debug) {
        addUsers(users, context.getListener(), context.getRun(), env, to, cc, bcc, debug);
    }

    /** If set, send to known users who lack {@link Item#READ} access to the job. */
    static /* not final */ boolean SEND_TO_USERS_WITHOUT_READ = Boolean.getBoolean(MailSender.class.getName() + ".SEND_TO_USERS_WITHOUT_READ");
    /** If set, send to unknown users. */
    static /* not final */ boolean SEND_TO_UNKNOWN_USERS = Boolean.getBoolean(MailSender.class.getName() + ".SEND_TO_UNKNOWN_USERS");

    private static void addUsers(final Set<User> users, final TaskListener listener, @CheckForNull Run<?,?> run, final EnvVars env,
        final Set<InternetAddress> to, final Set<InternetAddress> cc, final Set<InternetAddress> bcc, final IDebug debug) {
        for (final User user : users) {
            if (EmailRecipientUtils.isExcludedRecipient(user, listener)) {
                debug.send("User %s is an excluded recipient.", user.getFullName());
            } else {
                final String userAddress = EmailRecipientUtils.getUserConfiguredEmail(user);
                if (userAddress != null) {
                    if (Jenkins.get().isUseSecurity()) {
                        try {
                            Authentication auth = user.impersonate2();
                            if (run != null && !run.getACL().hasPermission2(auth, Item.READ)) {
                                if (SEND_TO_USERS_WITHOUT_READ) {
                                    listener.getLogger().printf("Warning: user %s has no permission to view %s, but sending mail anyway%n", userAddress, run.getFullDisplayName());
                                } else {
                                    listener.getLogger().printf("Not sending mail to user %s with no permission to view %s", userAddress, run.getFullDisplayName());
                                    continue;
                                }
                            }
                        } catch (AuthenticationException x) {
                            
                            if (SEND_TO_UNKNOWN_USERS || ExtendedEmailPublisher.descriptor().isAllowUnregisteredEnabled() ) {
                                listener.getLogger().printf("Warning: %s is not a recognized user, but sending mail anyway%n", userAddress);
                            } else {
                                listener.getLogger().printf("Not sending mail to unregistered user %s because your SCM"
                                        + " claimed this was associated with a user ID ‘", userAddress);
                                try {
                                    listener.hyperlink('/' + user.getUrl(), user.getDisplayName());
                                } catch (IOException ignored) {
                                }
                                listener.getLogger().printf("' which your security realm does not recognize; you may need" +
                                        " changes in your SCM plugin%n");
                                continue;
                            }
                        }
                    }
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
