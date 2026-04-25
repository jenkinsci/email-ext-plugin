package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailThrottlerTest {

    private EmailThrottler throttler;

    @BeforeEach
    void setUp() {
        throttler = new EmailThrottler();
    }

    @Test
    void isThrottlingLimitNotExceededInitially() {
        assertFalse(throttler.isThrottlingLimitExceeded());
    }

    @Test
    void isThrottlingLimitExceededAfterLimit() {
        for (int i = 0; i < EmailThrottler.THROTTLING_LIMIT; i++) {
            throttler.incrementEmailCount();
        }
        assertTrue(throttler.isThrottlingLimitExceeded());
    }

    @Test
    void resetEmailCountResetsCorrectly() {
        for (int i = 0; i < EmailThrottler.THROTTLING_LIMIT; i++) {
            throttler.incrementEmailCount();
        }
        assertTrue(throttler.isThrottlingLimitExceeded());
        throttler.resetEmailCount();
        assertFalse(throttler.isThrottlingLimitExceeded());
    }

    @Test
    void getInstanceReturnsSameInstance() {
        EmailThrottler instance1 = EmailThrottler.getInstance();
        EmailThrottler instance2 = EmailThrottler.getInstance();
        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }
}
