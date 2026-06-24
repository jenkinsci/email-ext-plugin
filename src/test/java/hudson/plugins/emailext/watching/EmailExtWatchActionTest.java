package hudson.plugins.emailext.watching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.trigger.AlwaysTrigger;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.Mailer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.MockedStatic;

/**
 * Tests the class {@link EmailExtWatchAction}.
 *
 * @author Akash Manna
 */
@WithJenkins
class EmailExtWatchActionTest {

    private JenkinsRule j;
    private FreeStyleProject project;
    private EmailExtWatchAction action;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        this.j = rule;
        project = j.createFreeStyleProject("test-project");
        action = new EmailExtWatchAction(project);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ)
                .everywhere()
                .to("alice"));
    }

    @Test
    void testBasicProperties() {
        assertNull(action.getIconFileName());
        assertEquals("emailExtWatch", action.getUrlName());
        assertEquals(project, action.getProject());
        assertEquals(hudson.plugins.emailext.Messages.EmailExtWatchAction_DisplayName(), action.getDisplayName());
    }

    @Test
    void testGetPublisher() throws Exception {
        assertNull(action.getPublisher());

        ArtifactArchiver archiver = new ArtifactArchiver("*.jar");
        project.getPublishersList().add(archiver);
        assertNull(action.getPublisher());

        ExtendedEmailPublisher publisher = new ExtendedEmailPublisher();
        project.getPublishersList().add(publisher);
        assertEquals(publisher, action.getPublisher());
    }

    @Test
    void testGetJobProperty() throws Exception {
        EmailExtWatchJobProperty prop = action.getJobProperty();
        assertNotNull(prop);
        assertEquals(prop, project.getProperty(EmailExtWatchJobProperty.class));

        assertEquals(prop, action.getJobProperty());
    }

    @Test
    void testUserProperty() throws Exception {
        EmailExtWatchAction.UserProperty.DescriptorImpl descriptor =
                new EmailExtWatchAction.UserProperty.DescriptorImpl();
        assertEquals("Extended Email Job Watching", descriptor.getDisplayName());

        EmailExtWatchAction.UserProperty prop = (EmailExtWatchAction.UserProperty) descriptor.newInstance((User) null);
        assertNotNull(prop);
        assertTrue(prop.getTriggers().isEmpty());

        List<EmailTrigger> triggers = new ArrayList<>();
        triggers.add(new AlwaysTrigger(Collections.emptyList(), "", "", "", "", "", 0, ""));
        prop = new EmailExtWatchAction.UserProperty("test-project", triggers);
        assertEquals("test-project", prop.getProjectName());
        assertEquals(1, prop.getTriggers().size());

        prop.setProjectName("another-project");
        assertEquals("another-project", prop.getProjectName());

        StaplerRequest2 req = mock(StaplerRequest2.class);
        JSONObject json = new JSONObject();
        when(req.bindJSONToList(EmailTrigger.class, json)).thenReturn(triggers);
        EmailExtWatchAction.UserProperty prop2 = descriptor.newInstance(req, json);
        assertEquals(1, prop2.getTriggers().size());

        EmailExtWatchAction.UserProperty prop3 = descriptor.newInstance((StaplerRequest2) null, json);
        assertTrue(prop3.getTriggers().isEmpty());
    }

    @Test
    void testUserSpecificMethods() throws Exception {
        User user = User.getById("alice", true);

        try (ACLContext ctx = ACL.as(user)) {
            Mailer.UserProperty mailerProp = new Mailer.UserProperty("alice@example.com");
            user.addProperty(mailerProp);
            assertNotNull(action.getMailerProperty());
            assertEquals("alice@example.com", action.getMailerProperty().getAddress());

            assertNull(action.getTriggers());
            assertFalse(action.isWatching());

            action.startWatching();
            assertTrue(action.isWatching(user));
            assertTrue(action.getJobProperty().isWatching(user));

            action.stopWatching();
            assertFalse(action.isWatching(user));

            List<EmailTrigger> triggers = new ArrayList<>();
            triggers.add(new AlwaysTrigger(Collections.emptyList(), "", "", "", "", "", 0, ""));
            EmailExtWatchAction.UserProperty userPropWrongProject =
                    new EmailExtWatchAction.UserProperty("wrong-project", triggers);
            user.addProperty(userPropWrongProject);

            assertNull(action.getTriggers());

            EmailExtWatchAction.UserProperty userProp =
                    new EmailExtWatchAction.UserProperty(project.getFullName(), triggers);
            user.addProperty(userProp);

            assertEquals(triggers, action.getTriggers());
            assertTrue(action.isWatching());
            assertEquals(
                    hudson.plugins.emailext.Messages.EmailExtWatchAction_DisplayNameWatching(),
                    action.getDisplayName());
        }
    }

    @Test
    void testIsWatching_EmptyTriggers() throws Exception {
        User user = User.getById("alice", true);
        try (ACLContext ctx = ACL.as(user)) {
            EmailExtWatchAction.UserProperty userProp =
                    new EmailExtWatchAction.UserProperty(project.getFullName(), new ArrayList<>());
            user.addProperty(userProp);

            assertNotNull(action.getTriggers());
            assertTrue(action.getTriggers().isEmpty());

            assertFalse(action.isWatching());
        }
    }

    @Test
    void testMethodsWithNullUser() throws Exception {
        try (ACLContext ctx = ACL.as2(Jenkins.ANONYMOUS2)) {
            assertNull(action.getTriggers());
            assertNull(action.getMailerProperty());
            assertFalse(action.isWatching());

            action.startWatching();
            action.stopWatching();
        }
    }

    @Test
    void testDoStopWatching() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        if (descriptor == null) {
            descriptor = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getExtensionList(Descriptor.class).add(0, descriptor);
        }
        descriptor.setWatchingEnabled(true);

        AlwaysTrigger.DescriptorImpl alwaysDesc = new AlwaysTrigger.DescriptorImpl();
        j.jenkins.getExtensionList(Descriptor.class).add(0, alwaysDesc);

        User user = User.getById("alice", true);
        try (ACLContext ctx = ACL.as(user)) {
            List<EmailTrigger> triggers = new ArrayList<>();
            triggers.add(new AlwaysTrigger(Collections.emptyList(), "", "", "", "", "", 0, ""));
            EmailExtWatchAction.UserProperty userProp =
                    new EmailExtWatchAction.UserProperty(project.getFullName(), triggers);
            user.addProperty(userProp);

            action.startWatching();
            assertTrue(action.isWatching(user));
            assertFalse(userProp.getTriggers().isEmpty());

            StaplerRequest2 req = mock(StaplerRequest2.class);
            StaplerResponse2 rsp = mock(StaplerResponse2.class);

            action.doStopWatching(req, rsp);

            assertFalse(action.isWatching(user));
            assertTrue(userProp.getTriggers().isEmpty());
            verify(rsp).sendRedirect(project.getAbsoluteUrl());
        }
    }

    @Test
    void testDoConfigSubmit() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        if (descriptor == null) {
            descriptor = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getExtensionList(Descriptor.class).add(0, descriptor);
        }
        descriptor.setWatchingEnabled(true);

        AlwaysTrigger.DescriptorImpl alwaysDesc = new AlwaysTrigger.DescriptorImpl();
        j.jenkins.getExtensionList(Descriptor.class).add(0, alwaysDesc);

        User user = User.getById("alice", true);
        try (ACLContext ctx = ACL.as(user)) {
            Mailer.UserProperty mailerProp = new Mailer.UserProperty("alice@example.com");
            user.addProperty(mailerProp);

            StaplerRequest2 req = mock(StaplerRequest2.class);
            StaplerResponse2 rsp = mock(StaplerResponse2.class);
            JSONObject form = new JSONObject();
            when(req.getSubmittedForm()).thenReturn(form);

            List<EmailTrigger> triggers = new ArrayList<>();
            AlwaysTrigger watchableTrigger = new AlwaysTrigger(Collections.emptyList(), "", "", "", "", "", 0, "");
            triggers.add(watchableTrigger);

            when(req.bindJSONToList(EmailTrigger.class, form.get("triggers"))).thenReturn(triggers);

            action.doConfigSubmit(req, rsp);

            assertTrue(action.isWatching(user));
            EmailExtWatchAction.UserProperty userProp = user.getProperty(EmailExtWatchAction.UserProperty.class);
            assertNotNull(userProp);
            assertEquals(1, userProp.getTriggers().size());
            assertEquals(
                    "alice@example.com",
                    userProp.getTriggers().get(0).getEmail().getRecipientList());

            verify(rsp).sendRedirect(project.getAbsoluteUrl());
        }
    }

    @Test
    void testDoStopWatching_Disabled() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        if (descriptor == null) {
            descriptor = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getExtensionList(Descriptor.class).add(0, descriptor);
        }
        descriptor.setWatchingEnabled(false);

        User user = User.getById("alice", true);
        try (ACLContext ctx = ACL.as(user)) {
            List<EmailTrigger> triggers = new ArrayList<>();
            triggers.add(new AlwaysTrigger(Collections.emptyList(), "", "", "", "", "", 0, ""));
            EmailExtWatchAction.UserProperty userProp =
                    new EmailExtWatchAction.UserProperty(project.getFullName(), triggers);
            user.addProperty(userProp);

            StaplerRequest2 req = mock(StaplerRequest2.class);
            StaplerResponse2 rsp = mock(StaplerResponse2.class);

            action.doStopWatching(req, rsp);

            assertFalse(userProp.getTriggers().isEmpty());
            verify(rsp).sendRedirect(project.getAbsoluteUrl());
        }
    }

    @Test
    void testDoStopWatching_Anonymous() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        if (descriptor == null) {
            descriptor = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getExtensionList(Descriptor.class).add(0, descriptor);
        }
        descriptor.setWatchingEnabled(true);

        AlwaysTrigger.DescriptorImpl alwaysDesc = new AlwaysTrigger.DescriptorImpl();
        j.jenkins.getExtensionList(Descriptor.class).add(0, alwaysDesc);
        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(User::current).thenReturn(null);

            StaplerRequest2 req = mock(StaplerRequest2.class);
            StaplerResponse2 rsp = mock(StaplerResponse2.class);

            action.doStopWatching(req, rsp);

            verify(rsp).sendRedirect(project.getAbsoluteUrl());
        }
    }

    @Test
    void testDoStopWatching_NonMatchingProperty() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        if (descriptor == null) {
            descriptor = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getExtensionList(Descriptor.class).add(0, descriptor);
        }
        descriptor.setWatchingEnabled(true);

        AlwaysTrigger.DescriptorImpl alwaysDesc = new AlwaysTrigger.DescriptorImpl();
        j.jenkins.getExtensionList(Descriptor.class).add(0, alwaysDesc);
        User mockUser = mock(User.class);
        when(mockUser.getAllProperties()).thenReturn(Collections.emptyList());

        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(User::current).thenReturn(mockUser);

            StaplerRequest2 req = mock(StaplerRequest2.class);
            StaplerResponse2 rsp = mock(StaplerResponse2.class);

            action.doStopWatching(req, rsp);

            verify(rsp).sendRedirect(project.getAbsoluteUrl());
        }
    }

    @Test
    void testDoConfigSubmit_Disabled() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        if (descriptor == null) {
            descriptor = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getExtensionList(Descriptor.class).add(0, descriptor);
        }
        descriptor.setWatchingEnabled(false);

        User user = User.getById("alice", true);
        try (ACLContext ctx = ACL.as(user)) {
            StaplerRequest2 req = mock(StaplerRequest2.class);
            StaplerResponse2 rsp = mock(StaplerResponse2.class);

            action.doConfigSubmit(req, rsp);

            assertFalse(action.isWatching(user));
            EmailExtWatchAction.UserProperty userProp = user.getProperty(EmailExtWatchAction.UserProperty.class);
            if (userProp != null) {
                assertTrue(userProp.getTriggers() == null || userProp.getTriggers().isEmpty());
            }
            verify(rsp).sendRedirect(project.getAbsoluteUrl());
        }
    }

    @Test
    void testDoConfigSubmit_Anonymous() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        if (descriptor == null) {
            descriptor = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getExtensionList(Descriptor.class).add(0, descriptor);
        }
        descriptor.setWatchingEnabled(true);

        AlwaysTrigger.DescriptorImpl alwaysDesc = new AlwaysTrigger.DescriptorImpl();
        j.jenkins.getExtensionList(Descriptor.class).add(0, alwaysDesc);
        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(User::current).thenReturn(null);

            StaplerRequest2 req = mock(StaplerRequest2.class);
            StaplerResponse2 rsp = mock(StaplerResponse2.class);

            action.doConfigSubmit(req, rsp);

            verify(rsp).sendRedirect(project.getAbsoluteUrl());
        }
    }

    @Test
    void testDoConfigSubmit_NullMailerProperty() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        if (descriptor == null) {
            descriptor = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getExtensionList(Descriptor.class).add(0, descriptor);
        }
        descriptor.setWatchingEnabled(true);

        AlwaysTrigger.DescriptorImpl alwaysDesc = new AlwaysTrigger.DescriptorImpl();
        j.jenkins.getExtensionList(Descriptor.class).add(0, alwaysDesc);

        User mockUser = mock(User.class);
        when(mockUser.getProperty(Mailer.UserProperty.class)).thenReturn(null);

        try (MockedStatic<User> userMock = mockStatic(User.class)) {
            userMock.when(User::current).thenReturn(mockUser);

            StaplerRequest2 req = mock(StaplerRequest2.class);
            StaplerResponse2 rsp = mock(StaplerResponse2.class);
            JSONObject form = new JSONObject();
            when(req.getSubmittedForm()).thenReturn(form);

            List<EmailTrigger> triggers = new ArrayList<>();
            triggers.add(new AlwaysTrigger(Collections.emptyList(), "", "", "", "", "", 0, ""));
            when(req.bindJSONToList(EmailTrigger.class, form.get("triggers"))).thenReturn(triggers);

            action.doConfigSubmit(req, rsp);

            verify(mockUser, never()).addProperty(any(UserProperty.class));
            verify(rsp).sendRedirect(project.getAbsoluteUrl());
        }
    }

    @Test
    void testDoConfigSubmit_UnwatchableTrigger() throws Exception {
        ExtendedEmailPublisherDescriptor descriptor =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        if (descriptor == null) {
            descriptor = new ExtendedEmailPublisherDescriptor();
            j.jenkins.getExtensionList(Descriptor.class).add(0, descriptor);
        }
        descriptor.setWatchingEnabled(true);

        AlwaysTrigger.DescriptorImpl alwaysDesc = new AlwaysTrigger.DescriptorImpl();
        j.jenkins.getExtensionList(Descriptor.class).add(0, alwaysDesc);
        User user = User.getById("alice", true);
        try (ACLContext ctx = ACL.as(user)) {
            Mailer.UserProperty mailerProp = new Mailer.UserProperty("alice@example.com");
            user.addProperty(mailerProp);

            StaplerRequest2 req = mock(StaplerRequest2.class);
            StaplerResponse2 rsp = mock(StaplerResponse2.class);
            JSONObject form = new JSONObject();
            when(req.getSubmittedForm()).thenReturn(form);

            List<EmailTrigger> triggers = new ArrayList<>();
            EmailTrigger unwatchableTrigger = mock(EmailTrigger.class);
            EmailTriggerDescriptor triggerDesc = mock(EmailTriggerDescriptor.class);
            when(unwatchableTrigger.getDescriptor()).thenReturn(triggerDesc);
            when(triggerDesc.isWatchable()).thenReturn(false);
            triggers.add(unwatchableTrigger);

            when(req.bindJSONToList(EmailTrigger.class, form.get("triggers"))).thenReturn(triggers);

            action.doConfigSubmit(req, rsp);

            EmailExtWatchAction.UserProperty userProp = user.getProperty(EmailExtWatchAction.UserProperty.class);
            assertNotNull(userProp);
            assertTrue(userProp.getTriggers().isEmpty());
            verify(rsp).sendRedirect(project.getAbsoluteUrl());
        }
    }
}
