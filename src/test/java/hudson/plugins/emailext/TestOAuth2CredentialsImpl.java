package hudson.plugins.emailext;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.google.jenkins.plugins.credentials.oauth.OAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.StandardUsernameOAuth2Credentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.Secret;
import java.util.Collection;
import java.util.Collections;

/**
 * Test implementation of OAuth2 credentials for testing OAuth SMTP authentication.
 * This is a test-only utility class.
 */
public final class TestOAuth2CredentialsImpl extends BaseStandardCredentials
        implements StandardUsernameOAuth2Credentials<TestOAuth2CredentialsImpl.TestOAuth2Requirement> {

    private final String username;
    private final Secret accessToken;

    public TestOAuth2CredentialsImpl(
            CredentialsScope scope, String id, String description, String username, String token) {
        super(scope, id, description);
        this.username = username;
        this.accessToken = Secret.fromString(token);
    }

    @Override
    @NonNull
    public String getUsername() {
        return username;
    }

    @Override
    public Secret getAccessToken(TestOAuth2Requirement requirement) {
        return accessToken;
    }

    @Extension
    public static final class DescriptorImpl extends CredentialsDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Test OAuth2 Credentials";
        }
    }

    /**
     * OAuth2 scope requirement for SMTP token requests.
     */
    public static final class TestOAuth2Requirement extends OAuth2ScopeRequirement {
        private static final Collection<String> SCOPES = Collections.singleton("smtp.send");

        @Override
        public Collection<String> getScopes() {
            return SCOPES;
        }
    }
}
