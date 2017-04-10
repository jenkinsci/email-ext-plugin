package hudson.plugins.emailext.groovy.sandbox;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.lang.reflect.Method;

/**
 * {@link org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist} for a specific instance of {@link PrintStream}.
 */
public class PrintStreamInstanceWhitelist extends ObjectInstanceWhitelist<PrintStream> {

    public PrintStreamInstanceWhitelist(PrintStream instance) {
        super(instance);
    }

    @Override
    public boolean permitsMethod(@Nonnull Method method, @Nonnull Object receiver, @Nonnull Object[] args) {
        if (permitsInstance(receiver) && isClass(method.getDeclaringClass())) {
            String name = method.getName();
            return name.equals("write")
                    || name.equals("print")
                    || name.equals("println")
                    || name.equals("printf")
                    || name.equals("format")
                    || name.equals("append");
        }
        return false;
    }
}
