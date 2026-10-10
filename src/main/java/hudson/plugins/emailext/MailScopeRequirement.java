package hudson.plugins.emailext;

import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import java.util.Collection;

public class MailScopeRequirement extends GoogleOAuth2ScopeRequirement {

    @Override
    public Collection<String> getScopes() {
        return ImmutableList.of("https://mail.google.com/");
    }
}
