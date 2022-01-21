package hudson.plugins.emailext.groovy.sandbox;

import hudson.Extension;
import java.io.IOException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

//TODO should probably be moved into default lists in script-security
@Restricted(NoExternalUse.class)
@Extension
public class StaticJakartaMailWhitelist extends ProxyWhitelist {
    public StaticJakartaMailWhitelist() throws IOException {
        super(StaticWhitelist.from(StaticJakartaMailWhitelist.class.getResource("jakarta.mail.whitelist")));
    }
}
