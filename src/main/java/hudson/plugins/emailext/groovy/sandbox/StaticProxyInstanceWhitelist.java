package hudson.plugins.emailext.groovy.sandbox;

import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * {@link ObjectInstanceWhitelist} backed by a set of {@link StaticWhitelist}s.
 */
public class StaticProxyInstanceWhitelist extends ObjectInstanceWhitelist<Object> {
    private Whitelist[] proxies;

    public StaticProxyInstanceWhitelist(Object instance, @Nonnull String... resources) throws IOException {
        super(instance);
        proxies = new Whitelist[resources.length];
        for (int i = 0; i < resources.length; i++) {
            String resource = resources[i];
            proxies[i] = StaticWhitelist.from(getClass().getResource(resource));
        }
    }

    @Override
    public boolean permitsMethod(@Nonnull Method method, @Nonnull Object receiver, @Nonnull Object[] args) {
        if (permitsInstance(receiver)) {
            for (Whitelist proxy : proxies) {
                if (proxy.permitsMethod(method, receiver, args)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean permitsFieldGet(@Nonnull Field field, @Nonnull Object receiver) {
        if (permitsInstance(receiver)) {
            for (Whitelist proxy : proxies) {
                if (proxy.permitsFieldGet(field, receiver)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean permitsFieldSet(@Nonnull Field field, @Nonnull Object receiver, @CheckForNull Object value) {
        return false;
    }
}
