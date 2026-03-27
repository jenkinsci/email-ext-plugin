package hudson.plugins.emailext;

/**
 * Thrown when OAuth 2.0 token acquisition fails in {@link OAuthTokenProvider}.
 *
 * <p>Common causes:</p>
 * <ul>
 *   <li>Invalid or expired Client Secret</li>
 *   <li>Wrong Tenant ID or Client ID</li>
 *   <li>Missing {@code SMTP.Send} API permission or admin consent not granted</li>
 *   <li>Network error reaching the Microsoft Entra ID token endpoint</li>
 *   <li>Microsoft Entra ID service outage</li>
 * </ul>
 *
 * @see OAuthTokenProvider
 */
public class OAuthTokenException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code OAuthTokenException} with a detail message
     * and the underlying cause.
     *
     * @param message human-readable description of the failure
     * @param cause   the underlying exception, or {@code null} if none
     */
    public OAuthTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code OAuthTokenException} with a detail message only.
     *
     * @param message human-readable description of the failure
     */
    public OAuthTokenException(String message) {
        super(message);
    }
}
