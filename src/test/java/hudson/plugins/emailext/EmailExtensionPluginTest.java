package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.Plugin;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * Tests the class {@link EmailExtensionPlugin}.
 *
 * @author Akash Manna
 */
class EmailExtensionPluginTest {

    private static final String SMTP_SENDPARTIAL = "mail.smtp.sendpartial";
    private static final String SMTPS_SENDPARTIAL = "mail.smtps.sendpartial";
    private static final String PLUGIN_CLASS_NAME = EmailExtensionPlugin.class.getName();

    /**
     * Loads a fresh copy of {@link EmailExtensionPlugin} in an isolated
     * {@link ClassLoader} so its static initializer runs again.
     */
    private static Class<?> loadFreshPluginClass() throws ReflectiveOperationException {
        ClassLoader parent = EmailExtensionPluginTest.class.getClassLoader();
        ClassLoader isolated = new ClassLoader(parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(PLUGIN_CLASS_NAME)) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> alreadyLoaded = findLoadedClass(name);
                    if (alreadyLoaded != null) {
                        return alreadyLoaded;
                    }
                    String resourcePath = name.replace('.', '/') + ".class";
                    try (InputStream classBytes = parent.getResourceAsStream(resourcePath)) {
                        if (classBytes == null) {
                            throw new ClassNotFoundException(name);
                        }
                        byte[] bytecode = classBytes.readAllBytes();
                        Class<?> defined = defineClass(name, bytecode, 0, bytecode.length);
                        if (resolve) {
                            resolveClass(defined);
                        }
                        return defined;
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
            }
        };
        return Class.forName(PLUGIN_CLASS_NAME, true, isolated);
    }

    /**
     * Verifies that the initializer sets {@code mail.smtp.sendpartial} to
     * {@code "true"} when the property is absent.
     */
    @Test
    void staticInitializerSetsSmtpSendPartialToTrue() throws ReflectiveOperationException {
        String originalSmtp = System.getProperty(SMTP_SENDPARTIAL);
        String originalSmtps = System.getProperty(SMTPS_SENDPARTIAL);
        try {
            System.clearProperty(SMTP_SENDPARTIAL);
            System.clearProperty(SMTPS_SENDPARTIAL);

            loadFreshPluginClass();

            assertEquals("true", System.getProperty(SMTP_SENDPARTIAL), SMTP_SENDPARTIAL + " should be \"true\"");
        } finally {
            restoreProperty(SMTP_SENDPARTIAL, originalSmtp);
            restoreProperty(SMTPS_SENDPARTIAL, originalSmtps);
        }
    }

    /**
     * Verifies that the initializer sets {@code mail.smtps.sendpartial} to
     * {@code "true"} when the property is absent.
     */
    @Test
    void staticInitializerSetsSmtpsSendPartialToTrue() throws ReflectiveOperationException {
        String originalSmtp = System.getProperty(SMTP_SENDPARTIAL);
        String originalSmtps = System.getProperty(SMTPS_SENDPARTIAL);
        try {
            System.clearProperty(SMTP_SENDPARTIAL);
            System.clearProperty(SMTPS_SENDPARTIAL);

            loadFreshPluginClass();

            assertEquals("true", System.getProperty(SMTPS_SENDPARTIAL), SMTPS_SENDPARTIAL + " should be \"true\"");
        } finally {
            restoreProperty(SMTP_SENDPARTIAL, originalSmtp);
            restoreProperty(SMTPS_SENDPARTIAL, originalSmtps);
        }
    }

    /**
     * Verifies that a property value set <em>before</em> the initializer runs is
     * preserved – i.e. the initializer only writes the value when the property is absent.
     */
    @Test
    void staticInitializerDoesNotOverridePreexistingSmtpProperty() throws ReflectiveOperationException {
        String originalSmtp = System.getProperty(SMTP_SENDPARTIAL);
        String originalSmtps = System.getProperty(SMTPS_SENDPARTIAL);
        try {
            System.setProperty(SMTP_SENDPARTIAL, "false");
            System.clearProperty(SMTPS_SENDPARTIAL);

            loadFreshPluginClass();

            assertEquals(
                    "false",
                    System.getProperty(SMTP_SENDPARTIAL),
                    "Pre-existing value for " + SMTP_SENDPARTIAL + " must not be overwritten");

            assertEquals(
                    "true",
                    System.getProperty(SMTPS_SENDPARTIAL),
                    "Missing value for " + SMTPS_SENDPARTIAL + " must be defaulted to \"true\"");
        } finally {
            restoreProperty(SMTP_SENDPARTIAL, originalSmtp);
            restoreProperty(SMTPS_SENDPARTIAL, originalSmtps);
        }
    }

    /**
     * Verifies that neither send-partial property is overwritten when both are already
     * set to a custom value before the initializer logic runs.
     */
    @Test
    void staticInitializerDoesNotOverrideBothPreexistingProperties() throws ReflectiveOperationException {
        String originalSmtp = System.getProperty(SMTP_SENDPARTIAL);
        String originalSmtps = System.getProperty(SMTPS_SENDPARTIAL);
        try {
            System.setProperty(SMTP_SENDPARTIAL, "false");
            System.setProperty(SMTPS_SENDPARTIAL, "false");

            loadFreshPluginClass();

            assertEquals(
                    "false",
                    System.getProperty(SMTP_SENDPARTIAL),
                    "Pre-existing " + SMTP_SENDPARTIAL + " must not be overwritten");
            assertEquals(
                    "false",
                    System.getProperty(SMTPS_SENDPARTIAL),
                    "Pre-existing " + SMTPS_SENDPARTIAL + " must not be overwritten");
        } finally {
            restoreProperty(SMTP_SENDPARTIAL, originalSmtp);
            restoreProperty(SMTPS_SENDPARTIAL, originalSmtps);
        }
    }

    /**
     * Smoke test: confirms that {@link EmailExtensionPlugin} can be instantiated
     * without throwing any exception, and that the static initializer has already
     * fired (properties are set).
     */
    @Test
    void pluginCanBeInstantiated() throws ReflectiveOperationException {
        Class<?> freshPluginClass = loadFreshPluginClass();
        Object plugin = freshPluginClass.getDeclaredConstructor().newInstance();

        assertNotNull(plugin, "EmailExtensionPlugin instance must not be null");
        assertInstanceOf(Plugin.class, plugin, "EmailExtensionPlugin must extend hudson.Plugin");
    }

    private static void restoreProperty(String key, String originalValue) {
        if (originalValue != null) {
            System.setProperty(key, originalValue);
        } else {
            System.clearProperty(key);
        }
    }
}
