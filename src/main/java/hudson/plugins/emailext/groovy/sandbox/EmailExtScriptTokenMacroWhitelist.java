/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.emailext.groovy.sandbox;

import edu.umd.cs.findbugs.annotations.NonNull;
import groovy.lang.GroovyObject;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.ContentBuilder;
import hudson.plugins.emailext.plugins.content.EmailExtScript;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.AbstractWhitelist;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

/**
 * {@link Whitelist} for the {@link org.jenkinsci.plugins.tokenmacro.TokenMacro} expansion
 * in {@link hudson.plugins.emailext.plugins.content.EmailExtScript}.
 */
public class EmailExtScriptTokenMacroWhitelist extends AbstractWhitelist {

    private static final Logger LOGGER = Logger.getLogger(EmailExtScriptTokenMacroWhitelist.class.getName());

    /**
     * Caches the EnvVars for the duration of a single script execution.
     * Populated via beginExecution() and cleared via endExecution() in a
     * try-finally block in ScriptContent, ensuring no memory leak and no
     * script-side mutation (lives purely in Java, not in the Groovy Binding).
     */
    public static final ThreadLocal<EnvVars> ENV_CACHE = new ThreadLocal<>();

    private final List<TokenMacro> macros;

    public EmailExtScriptTokenMacroWhitelist() {
        List<TokenMacro> list = new ArrayList<>();
        list.addAll(TokenMacro.all());
        list.addAll(ContentBuilder.getPrivateMacros());
        this.macros = Collections.unmodifiableList(list);
    }

    /**
     * Must be called once before the sandboxed script runs.
     * Eagerly resolves and caches the EnvVars for this execution.
     */
    public static void beginExecution(Run<?, ?> build, TaskListener listener) {
        try {
            ENV_CACHE.set(build.getEnvironment(listener));
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, e, () -> "Failed to pre-load environment for " + build.getExternalizableId());
        }
    }

    /**
     * Must be called in a finally block after the sandboxed script finishes.
     * Clears the cache to prevent any cross-execution leakage.
     */
    public static void endExecution() {
        ENV_CACHE.remove();
    }

    @Override
    public boolean permitsMethod(@NonNull Method method, @NonNull Object receiver, @NonNull Object[] args) {
        if (method.getDeclaringClass() == GroovyObject.class
                && receiver instanceof EmailExtScript script
                && "invokeMethod".equals(method.getName())
                && args.length > 0) {
            String name = String.valueOf(args[0]);

            for (TokenMacro m : macros) {
                if (m.acceptsMacroName(name)) {
                    return true;
                }
            }

            // Check environment — use cached EnvVars if available
            EnvVars vars = ENV_CACHE.get();
            if (vars != null) {
                return vars.containsKey(name);
            }

            // Fallback: no cache available, resolve directly
            Run<?, ?> build = (Run<?, ?>) script.getBinding().getVariable("build");
            TaskListener listener = (TaskListener) script.getBinding().getVariable("listener");
            try {
                return build.getEnvironment(listener).containsKey(name);
            } catch (IOException | InterruptedException e) {
                LOGGER.log(
                        Level.WARNING,
                        e,
                        () -> "Failed to expand environment when evaluating " + name + " on "
                                + build.getExternalizableId());
            }
        }
        return false;
    }
}
