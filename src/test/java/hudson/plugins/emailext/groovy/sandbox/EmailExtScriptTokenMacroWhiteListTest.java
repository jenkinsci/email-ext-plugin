package hudson.plugins.emailext.groovy.sandbox;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import groovy.lang.Binding;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.content.EmailExtScript;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class EmailExtScriptTokenMacroWhiteListTest {

    private EmailExtScriptTokenMacroWhitelist whitelist;
    private EmailExtScript mockScript;
    private Binding binding;
    private Run<?, ?> mockRun;
    private TaskListener mockListener;
    private EnvVars mockEnvVars;
    private Method invokeMethod;

    @BeforeEach
    public void setUp(JenkinsRule j) throws Exception {

        whitelist = new EmailExtScriptTokenMacroWhitelist();
        mockScript = mock(EmailExtScript.class);
        binding = new Binding();
        mockRun = mock(Run.class);
        mockListener = mock(TaskListener.class);
        mockEnvVars = new EnvVars();

        mockEnvVars.put("CACHED_VAR", "some_value");

        binding.setVariable("build", mockRun);
        binding.setVariable("listener", mockListener);
        when(mockScript.getBinding()).thenReturn(binding);

        // Get the invokeMethod reflection reference for the test
        invokeMethod = groovy.lang.GroovyObject.class.getMethod("invokeMethod", String.class, Object.class);
    }

    @Test
    public void testPermitsMethod_CachesEnvironmentVariables() throws Exception {

        when(mockRun.getEnvironment(mockListener)).thenReturn(mockEnvVars);

        Object[] args1 = new Object[] {"CACHED_VAR"};
        boolean result1 = whitelist.permitsMethod(invokeMethod, mockScript, args1);

        assertTrue(result1, "Should permit access to an existing environment variable");
        verify(mockRun, times(1)).getEnvironment(mockListener); // Fetched exactly once

        Object[] args2 = new Object[] {"NON_EXISTENT_VAR"};
        boolean result2 = whitelist.permitsMethod(invokeMethod, mockScript, args2);

        assertFalse(result2, "Should not permit access to a non-existent variable");
        verify(mockRun, times(1)).getEnvironment(mockListener);

        assertTrue(binding.hasVariable("EMAILEXT_CACHED_ENV_VARS"));
    }

    @Test
    public void testPerformanceGain_PrintOnly() throws Exception {

        when(mockRun.getEnvironment(mockListener)).thenAnswer(invocation -> {
            Thread.sleep(5);
            return mockEnvVars;
        });

        int iterations = 100; // Simulating an email template resolving 100 variables

        long startOld = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {

            binding.getVariables().remove("EMAILEXT_CACHED_ENV_VARS");
            whitelist.permitsMethod(invokeMethod, mockScript, new Object[] {"SOME_VAR_" + i});
        }
        long timeOld = System.currentTimeMillis() - startOld;

        binding.getVariables().remove("EMAILEXT_CACHED_ENV_VARS");

        long startNew = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            whitelist.permitsMethod(invokeMethod, mockScript, new Object[] {"SOME_VAR_" + i});
        }
        long timeNew = System.currentTimeMillis() - startNew;

        System.out.println("--- Performance Benchmark (" + iterations + " macro evaluations) ---");
        System.out.println("Old Way (Fetching env every time): " + timeOld + " ms");
        System.out.println("New Way (Cached in Binding):       " + timeNew + " ms");

        assertTrue(timeNew < timeOld, "The cached execution should be significantly faster");
    }
}
