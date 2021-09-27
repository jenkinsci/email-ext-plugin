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
 * {@link Whitelist} for the {@link org.jenkinsci.plugins.tokenmacro.TokenMacro} expansion in {@link hudson.plugins.emailext.plugins.content.EmailExtScript}.
 */
public class EmailExtScriptTokenMacroWhitelist extends AbstractWhitelist {

    private final List<TokenMacro> macros;

    public EmailExtScriptTokenMacroWhitelist() {
        List<TokenMacro> list = new ArrayList<>();

        list.addAll(TokenMacro.all());
        list.addAll(ContentBuilder.getPrivateMacros());

        this.macros = Collections.unmodifiableList(list);
    }

    @Override
    public boolean permitsMethod(@NonNull Method method, @NonNull Object receiver, @NonNull Object[] args) {
        //method groovy.lang.GroovyObject invokeMethod java.lang.String java.lang.Object (SimpleTemplateScript2 BUILD_ID)
        if (method.getDeclaringClass() == GroovyObject.class
                && receiver instanceof EmailExtScript && "invokeMethod".equals(method.getName()) && args.length > 0) {
            EmailExtScript script = (EmailExtScript)receiver;
            String name = String.valueOf(args[0]);

            for (TokenMacro m : macros) {
                if (m.acceptsMacroName(name)) {
                    return true;
                }
            }
            //Else check environment
            Run<?,?> build = (Run<?,?>)script.getBinding().getVariable("build");
            TaskListener listener = (TaskListener)script.getBinding().getVariable("listener");
            try {
                EnvVars vars = build.getEnvironment(listener);
                return vars.containsKey(name);
            } catch (IOException | InterruptedException e) {
                Logger.getLogger(getClass().getName()).log(Level.WARNING, e, () -> "Failed to expand environment when evaluating " + name + " on " + build.getExternalizableId());
            }

        }
        return false;
    }
}
