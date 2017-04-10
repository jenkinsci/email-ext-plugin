package hudson.plugins.emailext.groovy.sandbox;

import hudson.Extension;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;

//TODO should probably be moved into default lists in script-security
@Restricted(NoExternalUse.class)
@Extension
public class StaticJavaxMailWhitelist extends ProxyWhitelist {
    public StaticJavaxMailWhitelist() throws IOException {
        super(StaticWhitelist.from(StaticJavaxMailWhitelist.class.getResource("javax.mail.whitelist")));
    }
}
