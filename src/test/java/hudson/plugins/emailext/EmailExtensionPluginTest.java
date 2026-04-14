package hudson.plugins.emailext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link EmailExtensionPlugin}.
 */
class EmailExtensionPluginTest {

    private String originalSmtp;
    private String originalSmtps;

    @BeforeEach
    void setUp() {
        originalSmtp = System.getProperty("mail.smtp.sendpartial");
        originalSmtps = System.getProperty("mail.smtps.sendpartial");
        System.clearProperty("mail.smtp.sendpartial");
        System.clearProperty("mail.smtps.sendpartial");
    }

    @AfterEach
    void tearDown() {
        if (originalSmtp != null) {
            System.setProperty("mail.smtp.sendpartial", originalSmtp);
        } else {
            System.clearProperty("mail.smtp.sendpartial");
        }
        if (originalSmtps != null) {
            System.setProperty("mail.smtps.sendpartial", originalSmtps);
        } else {
            System.clearProperty("mail.smtps.sendpartial");
        }
    }

    /**
     * Verifies that sendpartial properties are set to {@code true}
     * when they are not already defined.
     */
    @Test
    void whenPropertiesNotSet_shouldSetThemToTrue() {
        EmailExtensionPlugin.initializeSendPartialProperties();
        assertEquals("true", System.getProperty("mail.smtp.sendpartial"));
        assertEquals("true", System.getProperty("mail.smtps.sendpartial"));
    }

    /**
     * Verifies that pre-existing sendpartial property values
     * are not overridden by the plugin initializer.
     */
    @Test
    void whenPropertiesAlreadySet_shouldNotOverrideThem() {
        System.setProperty("mail.smtp.sendpartial", "false");
        System.setProperty("mail.smtps.sendpartial", "false");
        EmailExtensionPlugin.initializeSendPartialProperties();
        assertEquals("false", System.getProperty("mail.smtp.sendpartial"));
        assertEquals("false", System.getProperty("mail.smtps.sendpartial"));
    }
}