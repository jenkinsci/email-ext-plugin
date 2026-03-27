package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OAuthTokenProvider}.
 *
 * <p>Tests cover constructor validation, token expiry detection logic,
 * and the authority URL format. Full integration tests against a mock
 * Microsoft Entra ID endpoint (using WireMock) will be added in
 * GSoC 2026 Week 11.</p>
 */
class OAuthTokenProviderTest {

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------

    @Test
    void constructor_throwsOnNullTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider(null, "clientId", "clientSecret"));
    }

    @Test
    void constructor_throwsOnEmptyTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider("", "clientId", "clientSecret"));
    }

    @Test
    void constructor_throwsOnBlankTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider("   ", "clientId", "clientSecret"));
    }

    @Test
    void constructor_throwsOnNullClientId() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider("tenantId", null, "clientSecret"));
    }

    @Test
    void constructor_throwsOnNullClientSecret() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider("tenantId", "clientId", null));
    }

    @Test
    void constructor_succeedsWithValidArguments() {
        assertDoesNotThrow(() -> new OAuthTokenProvider("tenantId", "clientId", "clientSecret"));
    }

    // -------------------------------------------------------------------------
    // Authority URL
    // -------------------------------------------------------------------------

    @Test
    void getAuthorityUrl_containsTenantId() {
        OAuthTokenProvider provider = new OAuthTokenProvider("my-tenant-id", "clientId", "secret");
        String url = provider.getAuthorityUrl();
        assertTrue(url.contains("my-tenant-id"), "Authority URL should include the tenant ID");
        assertTrue(
                url.startsWith("https://login.microsoftonline.com/"),
                "Authority URL should use the Microsoft Entra ID base");
    }

    // -------------------------------------------------------------------------
    // Token expiry detection
    // -------------------------------------------------------------------------

    @Test
    void isTokenExpiringSoon_returnsTrueWhenNoCachedToken() {
        OAuthTokenProvider provider = new OAuthTokenProvider("t", "c", "s");
        assertTrue(provider.isTokenExpiringSoon(), "Should report expiring soon when no token has been acquired");
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    @Test
    void smtpScope_isCorrectOutlookScope() {
        assertEquals(
                "https://outlook.office365.com/.default",
                OAuthTokenProvider.SMTP_SCOPE,
                "SMTP scope must match Microsoft's required value for XOAUTH2");
    }

    @Test
    void authorityBase_pointsToMicrosoftEntraId() {
        assertEquals(
                "https://login.microsoftonline.com/",
                OAuthTokenProvider.AUTHORITY_BASE,
                "Authority base URL must point to Microsoft Entra ID");
    }

    @Test
    void refreshThreshold_isFiveMinutes() {
        assertEquals(5, OAuthTokenProvider.REFRESH_THRESHOLD_MINUTES, "Refresh threshold should be 5 minutes");
    }
}
