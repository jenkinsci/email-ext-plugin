package hudson.plugins.emailext.groovy.sandbox;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * {@link org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist} for a specific {@link java.util.Properties} instance.
 */
public class PropertiesInstanceWhitelist extends ObjectInstanceWhitelist<Properties> {

    public PropertiesInstanceWhitelist(Properties instance) {
        super(instance);
    }

    @Override
    public boolean permitsMethod(@Nonnull Method method, @Nonnull Object receiver, @Nonnull Object[] args) {
        if (permitsInstance(receiver) && isClass(method.getDeclaringClass())) {
            String name = method.getName();
            return name.equals("setProperty")
                    || name.equals("put")
                    || name.equals("getProperty")
                    || name.equals("get")
                    || name.equals("propertyNames")
                    || name.equals("stringPropertyNames")
                    || name.equals("list");
        }
        return false;
    }
}
