package hudson.plugins.emailext.groovy.sandbox;

import hudson.model.TaskListener;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * {@link org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist} for specific instances of {@link TaskListener}.
 */
public class TaskListenerInstanceWhitelist extends ObjectInstanceWhitelist<TaskListener> {

    public TaskListenerInstanceWhitelist(TaskListener instance) {
        super(instance);
    }

    @Override
    public boolean permitsMethod(@Nonnull Method method, @Nonnull Object receiver, @Nonnull Object[] args) {
        if (permitsInstance(receiver) && isClass(method.getDeclaringClass())) {
            String name = method.getName();
            return name.equals("getLogger")
                    || name.equals("hyperlink")
                    || name.equals("error")
                    || name.equals("fatalError");
        }
        return false;
    }

    @Override
    public boolean permitsFieldGet(@Nonnull Field field, @Nonnull Object receiver) {
        return false;
    }

    @Override
    public boolean permitsFieldSet(@Nonnull Field field, @Nonnull Object receiver, @CheckForNull Object value) {
        return false;
    }
}
