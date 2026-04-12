package hudson.plugins.emailext.groovy.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class EmailExtScriptTokenMacroWhiteListTest {

    private Run<?, ?> mockRun;
    private TaskListener mockListener;
    private EnvVars mockEnvVars;

    @BeforeEach
    void setUp() throws Exception {
        mockRun = mock(Run.class);
        mockListener = mock(TaskListener.class);
        mockEnvVars = new EnvVars();
        mockEnvVars.put("BUILD_ID", "42");
        when(mockRun.getEnvironment(mockListener)).thenReturn(mockEnvVars);
    }

    @AfterEach
    void tearDown() {
        // clean up ThreadLocal even if test fails
        EmailExtScriptTokenMacroWhitelist.endExecution();
    }

    @Test
    void testGetEnvironmentCalledOnlyOnce() throws Exception {
        EmailExtScriptTokenMacroWhitelist.beginExecution(mockRun, mockListener);

        EnvVars cached1 = EmailExtScriptTokenMacroWhitelist.ENV_CACHE.get();
        EnvVars cached2 = EmailExtScriptTokenMacroWhitelist.ENV_CACHE.get();
        EnvVars cached3 = EmailExtScriptTokenMacroWhitelist.ENV_CACHE.get();

        assertEquals(cached1, cached2);
        assertEquals(cached2, cached3);

        verify(mockRun, times(1)).getEnvironment(mockListener);
    }

    @Test
    void testCacheIsClearedAfterEndExecution() {
        EmailExtScriptTokenMacroWhitelist.beginExecution(mockRun, mockListener);
        EmailExtScriptTokenMacroWhitelist.endExecution();

        // Cache must be null after cleanup
        assertNull(EmailExtScriptTokenMacroWhitelist.ENV_CACHE.get());
    }

    @Test
    void testFallbackWhenCacheNotInitialized() {

        assertNull(EmailExtScriptTokenMacroWhitelist.ENV_CACHE.get());
    }

    @Test
    @Tag("performance")
    void testProfilingGain() throws Exception {
        // Simulates real getEnvironment() cost with busy-wait
        when(mockRun.getEnvironment(mockListener)).thenAnswer(invocation -> {
            long end = System.nanoTime() + 5_000_000L; // 5ms simulated I/O
            while (System.nanoTime() < end) {
                /* busy wait */
            }
            return mockEnvVars;
        });

        int iterations = 100;

        // Warm up JVM
        for (int i = 0; i < 5; i++) {
            runBaseline(iterations);
            runCached(iterations);
        }

        // Actual benchmark
        long startBaseline = System.nanoTime();
        runBaseline(iterations);
        long baselineMs = (System.nanoTime() - startBaseline) / 1_000_000;

        long startCached = System.nanoTime();
        runCached(iterations);
        long cachedMs = (System.nanoTime() - startCached) / 1_000_000;

        System.out.printf("Baseline (no cache): %d ms%n", baselineMs);
        System.out.printf("ThreadLocal cache:   %d ms%n", cachedMs);
        System.out.printf("Reduction:           %.0f%%%n", (1.0 - (double) cachedMs / baselineMs) * 100);
    }

    private void runBaseline(int iterations) throws Exception {
        for (int i = 0; i < iterations; i++) {
            mockRun.getEnvironment(mockListener);
        }
    }

    private void runCached(int iterations) throws Exception {
        EmailExtScriptTokenMacroWhitelist.beginExecution(mockRun, mockListener);
        try {
            for (int i = 0; i < iterations; i++) {
                EmailExtScriptTokenMacroWhitelist.ENV_CACHE.get();
            }
        } finally {
            EmailExtScriptTokenMacroWhitelist.endExecution();
        }
    }
}
