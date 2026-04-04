package hudson.plugins.emailext.groovy.sandbox;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaticProxyInstanceWhitelistTest {

    private Object instance;

    @BeforeEach
    void setUp() {
        instance = new Object();
    }

    @Test
    void constructorThrowsOnMissingResource() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StaticProxyInstanceWhitelist(instance, "/non/existent/resource.txt"));

        assertTrue(
                ex.getMessage().contains("/non/existent/resource.txt"),
                "Exception message should mention the missing resource path");
    }

    @Test
    void constructorThrowsOnOneOfMultipleMissingResources() {
        // verifies the check works when multiple resources are passed
        // and one of them is missing
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StaticProxyInstanceWhitelist(
                        instance, "/non/existent/resource.txt", "/another/missing/resource.txt"));

        assertTrue(
                ex.getMessage().contains("not found on classpath"),
                "Exception message should indicate the resource was not found");
    }

    @Test
    void permitsMethodReturnsFalseForUnwhitelistedMethod() throws Exception {
        // empty whitelist — no resources, so nothing is permitted
        StaticProxyInstanceWhitelist whitelist = new StaticProxyInstanceWhitelist(instance);

        Method method = String.class.getMethod("toString");

        assertFalse(
                whitelist.permitsMethod(method, instance, new Object[0]),
                "permitsMethod should return false when no proxies permit it");
    }

    @Test
    void permitsMethodReturnsFalseForWrongReceiver() throws Exception {
        StaticProxyInstanceWhitelist whitelist = new StaticProxyInstanceWhitelist(instance);

        Method method = String.class.getMethod("toString");
        Object differentInstance = new Object();

        assertFalse(
                whitelist.permitsMethod(method, differentInstance, new Object[0]),
                "permitsMethod should return false when receiver is not the whitelisted instance");
    }

    @Test
    void permitsFieldGetReturnsFalseForUnwhitelistedField() throws Exception {
        StaticProxyInstanceWhitelist whitelist = new StaticProxyInstanceWhitelist(instance);

        Field field = String.class.getDeclaredField("value");

        assertFalse(
                whitelist.permitsFieldGet(field, instance),
                "permitsFieldGet should return false when no proxies permit it");
    }

    @Test
    void permitsFieldSetAlwaysReturnsFalse() throws Exception {
        StaticProxyInstanceWhitelist whitelist = new StaticProxyInstanceWhitelist(instance);

        Field field = String.class.getDeclaredField("value");

        assertFalse(
                whitelist.permitsFieldSet(field, instance, null),
                "permitsFieldSet should always return false unconditionally");
    }
}
