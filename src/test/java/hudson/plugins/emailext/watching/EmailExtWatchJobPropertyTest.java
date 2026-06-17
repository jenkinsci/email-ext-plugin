package hudson.plugins.emailext.watching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.Job;
import hudson.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Tests the class {@link EmailExtWatchJobProperty}.
 *
 * @author Akash Manna
 */
class EmailExtWatchJobPropertyTest {

    private EmailExtWatchJobProperty property;

    @BeforeEach
    void setUp() {
        property = new EmailExtWatchJobProperty();
    }

    @Test
    void testGetJobActions() {
        Job<?, ?> mockJob = mock(Job.class);
        assertTrue(property.getJobActions(mockJob).isEmpty(), "Job actions should be empty");
    }

    @Test
    void testGetWatchers() {
        assertTrue(property.getWatchers().isEmpty(), "Initial watchers should be empty");
    }

    @Test
    void testAddWatcher() {
        User user1 = mock(User.class);
        when(user1.getId()).thenReturn("user1");

        property.addWatcher(user1);
        assertEquals(1, property.getWatchers().size(), "Should have 1 watcher");
        assertTrue(property.getWatchers().contains("user1"), "Watcher list should contain user1");

        property.addWatcher(user1);
        assertEquals(1, property.getWatchers().size(), "Should still have 1 watcher");
    }

    @Test
    void testRemoveWatcher() {
        User user1 = mock(User.class);
        when(user1.getId()).thenReturn("user1");

        User user2 = mock(User.class);
        when(user2.getId()).thenReturn("user2");

        property.addWatcher(user1);
        property.addWatcher(user2);
        assertEquals(2, property.getWatchers().size());

        property.removeWatcher(user1);
        assertEquals(1, property.getWatchers().size(), "Should have 1 watcher after removal");
        assertFalse(property.getWatchers().contains("user1"), "Watcher list should not contain user1");
        assertTrue(property.getWatchers().contains("user2"), "Watcher list should contain user2");

        User user3 = mock(User.class);
        when(user3.getId()).thenReturn("user3");
        property.removeWatcher(user3);
        assertEquals(1, property.getWatchers().size(), "Size should not change when removing non-existent user");
    }

    @Test
    void testIsWatching() {
        User user1 = mock(User.class);
        when(user1.getId()).thenReturn("user1");

        User user2 = mock(User.class);
        when(user2.getId()).thenReturn("user2");

        property.addWatcher(user1);

        assertTrue(property.isWatching(user1), "User1 should be watching");
        assertFalse(property.isWatching(user2), "User2 should not be watching");
    }

    @Test
    void testDescriptor() throws Exception {
        EmailExtWatchJobProperty.DescriptorImpl descriptor = new EmailExtWatchJobProperty.DescriptorImpl();
        assertTrue(descriptor.isApplicable(Job.class), "Should be applicable to any Job");
        assertEquals("", descriptor.getDisplayName(), "Display name should be empty");
        assertNull(descriptor.newInstance((StaplerRequest2) null, null), "New instance should be null");
    }
}
