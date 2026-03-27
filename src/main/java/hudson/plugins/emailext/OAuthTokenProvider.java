package hudson.plugins.emailext;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides OAuth 2.0 access tokens for Microsoft Outlook SMTP authentication.
 *
 * <p>Implements the Client Credentials Flow (RFC 6749 §4.4) to acquire
 * short-lived Bearer tokens from Microsoft Entra ID (Azure AD).
 * Tokens are cached in memory and a proactive refresh is triggered when
 * the token is within {@value #REFRESH_THRESHOLD_MINUTES} minutes of expiry.</p>
 *
 * <p>This class is designed as the token-management foundation for the
 * GSoC 2026 project: <em>Jenkins Email Notifications via Outlook SMTP
 * with OAuth</em>. Full MSAL4J integration and XOAUTH2 SASL wiring into
 *  will follow in subsequent commits once the MSAL4J
 * dependency is added to {@code pom.xml}.</p>
 *
 * <p>Typical usage (once integrated):</p>
 * <pre>
 *   OAuthTokenProvider provider =
 *       new OAuthTokenProvider(tenantId, clientId, clientSecret);
 *   String token = provider.getAccessToken();
 *   // Pass token to XOAuth2Authenticator for JavaMail SMTP session
 * </pre>
 *
 * @see OAuthTokenException
 * @see <a href="https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-client-creds-grant-flow">
 *      Microsoft Client Credentials Flow</a>
 * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-java">
 *      MSAL4J library</a>
 * @see <a href="https://tools.ietf.org/html/rfc7628">RFC 7628 — SASL XOAUTH2</a>
 */
public class OAuthTokenProvider {

    private static final Logger LOGGER = Logger.getLogger(OAuthTokenProvider.class.getName());

    /**
     * Base URL for the Microsoft Entra ID token endpoint.
     * Full URL: {@code AUTHORITY_BASE + tenantId + "/oauth2/v2.0/token"}
     */
    static final String AUTHORITY_BASE = "https://login.microsoftonline.com/";

    /**
     * OAuth 2.0 scope that grants SMTP.Send permission for
     * Outlook / Exchange Online via the XOAUTH2 SASL mechanism.
     */
    static final String SMTP_SCOPE = "https://outlook.office365.com/.default";

    /**
     * Number of minutes before token expiry at which a proactive
     * refresh should be triggered by {@code TokenHealthMonitor}.
     */
    static final int REFRESH_THRESHOLD_MINUTES = 5;

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;

    /**
     * The expiry time of the currently cached token.
     * {@code null} if no token has been acquired yet.
     */
    private volatile Date tokenExpiresOn;

    /**
     * The currently cached Bearer access token string.
     * {@code null} if no token has been acquired yet.
     */
    private volatile String cachedAccessToken;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an {@code OAuthTokenProvider} for the given Azure App
     * Registration credentials.
     *
     * <p>All three values are required. They map directly to the fields
     * visible in the Azure Portal under <em>App registrations → your app →
     * Overview / Certificates &amp; secrets</em>.</p>
     *
     * @param tenantId     Azure AD Directory (Tenant) ID — e.g.
     *                     {@code "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"}
     * @param clientId     Application (Client) ID of the registered app
     * @param clientSecret Client Secret value (not the Secret ID)
     * @throws IllegalArgumentException if any parameter is {@code null} or blank
     */
    public OAuthTokenProvider(String tenantId, String clientId, String clientSecret) {

        if (isBlank(tenantId)) {
            throw new IllegalArgumentException("tenantId must not be null or empty");
        }
        if (isBlank(clientId)) {
            throw new IllegalArgumentException("clientId must not be null or empty");
        }
        if (isBlank(clientSecret)) {
            throw new IllegalArgumentException("clientSecret must not be null or empty");
        }
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a valid OAuth 2.0 Bearer access token for Outlook SMTP.
     *
     * <p>Returns the in-memory cached token if it is still valid and not
     * within the proactive-refresh window. Otherwise calls
     * {@link #refreshToken()} to acquire a new one from Microsoft Entra ID.</p>
     *
     * @return a non-null, non-empty Bearer access token string
     * @throws OAuthTokenException if token acquisition fails for any reason
     */
    public String getAccessToken() throws OAuthTokenException {
        if (cachedAccessToken != null && !isTokenExpiringSoon()) {
            LOGGER.fine("Returning cached OAuth token" + " (expires: " + tokenExpiresOn + ")");
            return cachedAccessToken;
        }
        return refreshToken();
    }

    /**
     * Forces acquisition of a fresh token from Microsoft Entra ID,
     * bypassing the in-memory cache.
     *
     * <p>Called proactively by {@code TokenHealthMonitor} when
     * {@link #isTokenExpiringSoon()} returns {@code true}, ensuring tokens
     * are always valid before an email send is attempted.</p>
     *
     * <p><strong>Implementation note:</strong> Full MSAL4J wiring will replace
     * the stub below once the {@code com.microsoft.azure:msal4j} dependency
     * is added to {@code pom.xml}. The method signature, exception type, and
     * caching contract are final.</p>
     *
     * @return a fresh Bearer access token string
     * @throws OAuthTokenException if Microsoft Entra ID returns an error
     */
    public String refreshToken() throws OAuthTokenException {
        LOGGER.info("Acquiring new OAuth token for tenant: " + tenantId);
        try {
            /*
             * TODO (GSoC 2026, Week 3-4): Replace this stub with real
             * MSAL4J acquisition:
             *
             *   ConfidentialClientApplication app =
             *       ConfidentialClientApplication
             *           .builder(clientId,
             *               ClientCredentialFactory.createFromSecret(clientSecret))
             *           .authority(AUTHORITY_BASE + tenantId)
             *           .build();
             *
             *   Set<String> scopes = Collections.singleton(SMTP_SCOPE);
             *   ClientCredentialParameters params =
             *       ClientCredentialParameters.builder(scopes).build();
             *
             *   IAuthenticationResult result =
             *       app.acquireToken(params).get();
             *
             *   this.cachedAccessToken = result.accessToken();
             *   this.tokenExpiresOn    = result.expiresOnDate();
             *   return this.cachedAccessToken;
             */
            throw new OAuthTokenException(
                    "MSAL4J integration pending — " + "add msal4j dependency to pom.xml (GSoC 2026 Week 3)", null);

        } catch (OAuthTokenException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error acquiring OAuth token", e);
            throw new OAuthTokenException("Failed to acquire OAuth token: " + e.getMessage(), e);
        }
    }

    /**
     * Returns {@code true} if the cached token is absent or will expire
     * within {@link #REFRESH_THRESHOLD_MINUTES} minutes.
     *
     * <p>Used by {@code TokenHealthMonitor} to decide whether to trigger
     * a proactive refresh before the next email send.</p>
     *
     * @return {@code true} if a refresh is recommended
     */
    public boolean isTokenExpiringSoon() {
        if (tokenExpiresOn == null) {
            return true;
        }
        long expiryMs = tokenExpiresOn.getTime();
        long nowMs = System.currentTimeMillis();
        long thresholdMs = (long) REFRESH_THRESHOLD_MINUTES * 60 * 1000;
        boolean expiringSoon = (expiryMs - nowMs) < thresholdMs;
        if (expiringSoon) {
            LOGGER.fine("OAuth token expiring soon" + " (expires: " + tokenExpiresOn + ")");
        }
        return expiringSoon;
    }

    /**
     * Returns the Authority URL that will be used when requesting tokens.
     * Exposed for unit testing.
     *
     * @return full authority URL string
     */
    String getAuthorityUrl() {
        return AUTHORITY_BASE + tenantId;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
