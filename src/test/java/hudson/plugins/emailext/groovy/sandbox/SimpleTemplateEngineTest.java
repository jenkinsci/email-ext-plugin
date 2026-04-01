package hudson.plugins.emailext.groovy.sandbox;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.text.Template;
import java.io.NotSerializableException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link SimpleTemplateEngine}
 */
class SimpleTemplateEngineTest {

    private SimpleTemplateEngine engine() {
        return new SimpleTemplateEngine(new GroovyShell(), false);
    }

    private static Writer writerThrowing(RuntimeException ex) {
        return new StringWriter() {
            @Override public void write(int c) { throw ex; }
            @Override public void write(char[] buf, int off, int len) { throw ex; }
            @Override public void write(String str) { throw ex; }
            @Override public void write(String str, int off, int len) { throw ex; }
        };
    }

    @Test
    void basicTemplateRenderingProducesExpectedOutput() throws Exception {
        
        Template t = engine().createTemplate("Hello ${name}!");
        Map<String, Object> binding = new HashMap<>();
        binding.put("name", "World");
        StringWriter sw = new StringWriter();
        t.make(binding).writeTo(sw);
        assertTrue(sw.toString().equals("Hello World!"));
    }

    @Test
    void notSerializableExceptionProducesActionableMessage() throws Exception {
        Template t = engine().createTemplate("Hello!");

        GroovyRuntimeException ex = assertThrows(GroovyRuntimeException.class, () ->
                t.make(null).writeTo(
                        writerThrowing(new RuntimeException(new NotSerializableException("java.io.PrintWriter")))));

        assertTrue(ex.getMessage().contains("node"), "Expected message to mention Pipeline node block");
        assertTrue(ex.getMessage().contains("@NonCPS"), "Expected message to mention @NonCPS");
        assertTrue(ex.getMessage().contains("serialization"), "Expected message to mention serialization");
    }

    @Test
    void unrelatedRuntimeExceptionIsRethrownAsIs() throws Exception {
        Template t = engine().createTemplate("Hello!");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                t.make(null).writeTo(
                        writerThrowing(new RuntimeException(new IllegalStateException("something unrelated")))));

        assertTrue(ex.getCause() instanceof IllegalStateException,
                "Unrelated exception cause should be preserved and not wrapped");
    }
}