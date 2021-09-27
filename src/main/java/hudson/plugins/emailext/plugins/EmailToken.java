package hudson.plugins.emailext.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.java.sezpoz.Indexable;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

@Indexable(type=TokenMacro.class)
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@Documented
public @interface EmailToken {
    /* we don't need anything in here, just the annotation itself */
}
