package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OAuthTokenProvider}.
 */
public class OAuthTokenProviderTest {

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------

    @Test
    public void constructor_throwsOnNullTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider(null, "clientId", "clientSecret"));
    }

    @Test
    public void constructor_throwsOnEmptyTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider("", "clientId", "clientSecret"));
    }

    @Test
    public void constructor_throwsOnBlankTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider("   ", "clientId", "clientSecret"));
    }

    @Test
    public void constructor_throwsOnNullClientId() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider("tenantId", null, "clientSecret"));
    }

    @Test
    public void constructor_throwsOnNullClientSecret() {
        assertThrows(IllegalArgumentException.class, () -> new OAuthTokenProvider("tenantId", "clientId", null));
    }

    @Test
    public void constructor_succeedsWithValidArguments() {
        assertDoesNotThrow(() -> new OAuthTokenProvider("tenantId", "clientId", "clientSecret"));
    }

    // -------------------------------------------------------------------------
    // Authority URL
    // -------------------------------------------------------------------------

    @Test
    public void getAuthorityUrl_containsTenantId() {
        OAuthTokenProvider provider = new OAuthTokenProvider("my-tenant-id", "clientId", "secret");

        String url = provider.getAuthorityUrl();

        assertTrue(url.contains("my-tenant-id"));
        assertTrue(url.startsWith("https://login.microsoftonline.com/"));
    }

    // -------------------------------------------------------------------------
    // Token expiry detection
    // -------------------------------------------------------------------------

    @Test
    public void isTokenExpiringSoon_returnsTrueWhenNoCachedToken() {
        OAuthTokenProvider provider = new OAuthTokenProvider("t", "c", "s");

        assertTrue(provider.isTokenExpiringSoon());
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    @Test
    public void smtpScope_isCorrectOutlookScope() {
        assertEquals("https://outlook.office365.com/.default", OAuthTokenProvider.SMTP_SCOPE);
    }

    @Test
    public void authorityBase_pointsToMicrosoftEntraId() {
        assertEquals("https://login.microsoftonline.com/", OAuthTokenProvider.AUTHORITY_BASE);
    }

    @Test
    public void refreshThreshold_isFiveMinutes() {
        assertEquals(5, OAuthTokenProvider.REFRESH_THRESHOLD_MINUTES);
    }

    @Test
    public void constructor_messageOnly_setsMessage() {
        OAuthTokenException ex = new OAuthTokenException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }
}
