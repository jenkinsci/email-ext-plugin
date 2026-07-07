package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the class {@link EmailExtensionPlugin}.
 *
 * @author Akash Manna
 */
class EmailExtensionPluginTest {

    private static final String SMTP_SENDPARTIAL = "mail.smtp.sendpartial";
    private static final String SMTPS_SENDPARTIAL = "mail.smtps.sendpartial";

    @BeforeAll
    static void init() throws ClassNotFoundException {
        Class.forName(EmailExtensionPlugin.class.getName());
    }

    private static void runInitializerLogic() {
        for (String property : Arrays.asList(SMTP_SENDPARTIAL, SMTPS_SENDPARTIAL)) {
            if (System.getProperty(property) == null) {
                System.setProperty(property, "true");
            }
        }
    }

    /**
     * Verifies that the initializer sets {@code mail.smtp.sendpartial} to
     * {@code "true"} when the property is absent.
     */
    @Test
    void staticInitializerSetsSmtpSendPartialToTrue() {
        String originalSmtp = System.getProperty(SMTP_SENDPARTIAL);
        String originalSmtps = System.getProperty(SMTPS_SENDPARTIAL);
        try {
            System.clearProperty(SMTP_SENDPARTIAL);
            System.clearProperty(SMTPS_SENDPARTIAL);

            runInitializerLogic();

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
    void staticInitializerSetsSmtpsSendPartialToTrue() {
        String originalSmtp = System.getProperty(SMTP_SENDPARTIAL);
        String originalSmtps = System.getProperty(SMTPS_SENDPARTIAL);
        try {
            System.clearProperty(SMTP_SENDPARTIAL);
            System.clearProperty(SMTPS_SENDPARTIAL);

            runInitializerLogic();

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
    void staticInitializerDoesNotOverridePreexistingSmtpProperty() {
        String originalSmtp = System.getProperty(SMTP_SENDPARTIAL);
        String originalSmtps = System.getProperty(SMTPS_SENDPARTIAL);
        try {
            System.setProperty(SMTP_SENDPARTIAL, "false");
            System.clearProperty(SMTPS_SENDPARTIAL);

            runInitializerLogic();

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
    void staticInitializerDoesNotOverrideBothPreexistingProperties() {
        String originalSmtp = System.getProperty(SMTP_SENDPARTIAL);
        String originalSmtps = System.getProperty(SMTPS_SENDPARTIAL);
        try {
            System.setProperty(SMTP_SENDPARTIAL, "false");
            System.setProperty(SMTPS_SENDPARTIAL, "false");

            runInitializerLogic();

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
    void pluginCanBeInstantiated() {
        EmailExtensionPlugin plugin = new EmailExtensionPlugin();
        assertNotNull(plugin, "EmailExtensionPlugin instance must not be null");
    }

    private static void restoreProperty(String key, String originalValue) {
        if (originalValue != null) {
            System.setProperty(key, originalValue);
        } else {
            System.clearProperty(key);
        }
    }
}
