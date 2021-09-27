package hudson.plugins.emailext.groovy.sandbox;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TaskListener;
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
    public boolean permitsMethod(@NonNull Method method, @NonNull Object receiver, @NonNull Object[] args) {
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
    public boolean permitsFieldGet(@NonNull Field field, @NonNull Object receiver) {
        return false;
    }

    @Override
    public boolean permitsFieldSet(@NonNull Field field, @NonNull Object receiver, @CheckForNull Object value) {
        return false;
    }
}
