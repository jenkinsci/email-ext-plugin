/*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hudson.plugins.emailext.groovy.sandbox;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.lang.Writable;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import hudson.plugins.emailext.plugins.content.ScriptContent;
import jenkins.util.SystemProperties;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes template source files substituting variables and expressions into
 * placeholders in a template source text to produce the desired output.
 * <p>
 * The template engine uses JSP style &lt;% %&gt; script and &lt;%= %&gt; expression syntax
 * or GString style expressions. The variable '<code>out</code>' is bound to the writer that the template
 * is being written to.
 * <p>
 * Frequently, the template source will be in a file but here is a simple
 * example providing the template as a string:
 * <pre>
 * def binding = [
 *     firstname : "Grace",
 *     lastname  : "Hopper",
 *     accepted  : true,
 *     title     : 'Groovy for COBOL programmers'
 * ]
 * def engine = new groovy.text.SimpleTemplateEngine()
 * def text = '''\
 * Dear &lt;%= firstname %&gt; $lastname,
 *
 * We &lt;% if (accepted) print 'are pleased' else print 'regret' %&gt; \
 * to inform you that your paper entitled
 * '$title' was ${ accepted ? 'accepted' : 'rejected' }.
 *
 * The conference committee.
 * '''
 * def template = engine.createTemplate(text).make(binding)
 * println template.toString()
 * </pre>
 * This example uses a mix of the JSP style and GString style placeholders
 * but you can typically use just one style if you wish. Running this
 * example will produce this output:
 * <pre>
 * Dear Grace Hopper,
 *
 * We are pleased to inform you that your paper entitled
 * 'Groovy for COBOL programmers' was accepted.
 *
 * The conference committee.
 * </pre>
 * The template engine can also be used as the engine for {@link groovy.servlet.TemplateServlet} by placing the
 * following in your <code>web.xml</code> file (plus a corresponding servlet-mapping element):
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;SimpleTemplate&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;groovy.servlet.TemplateServlet&lt;/servlet-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;template.engine&lt;/param-name&gt;
 *     &lt;param-value&gt;groovy.text.SimpleTemplateEngine&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/servlet&gt;
 * </pre>
 * In this case, your template source file should be HTML with the appropriate embedded placeholders.
 *
 * @author sam
 * @author Christian Stein
 * @author Paul King
 * @author Alex Tkachman
 */
public class SimpleTemplateEngine extends TemplateEngine {

    private static final Logger LOGGER = Logger.getLogger(SimpleTemplateEngine.class.getName());

    /**
     * Maximum size of template expansion we expect to produce.
     * Unit: bytes
     * Default: 1MB
     */
    // public for testing
    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "non final to make it editable from the script console (convenient to temporarily change the value without restarting)")
    public static int MAX_EXPANDED_SIZE_BYTES = SystemProperties.getInteger(SimpleTemplateEngine.class.getName() + ".MAX_EXPANDED_SIZE_BYTES", 1024 * 1024);

    private static AtomicInteger counter = new AtomicInteger(1);

    protected final GroovyShell groovyShell;
    private final boolean sandbox;

    @SuppressWarnings("lgtm[jenkins/unsafe-classes]")
    public SimpleTemplateEngine(GroovyShell groovyShell, boolean sandbox) {
        this.groovyShell = groovyShell;
        this.sandbox = sandbox;
    }

    public Template createTemplate(Reader reader) throws CompilationFailedException, IOException {
        return createTemplate(reader,"SimpleTemplateScript" + counter.getAndIncrement() + ".groovy");
    }

    public Template createTemplate(Reader reader, String fileName) throws CompilationFailedException, IOException {
        SimpleTemplate template = new SimpleTemplate(sandbox);
        template.script = parseScript(reader,fileName);
        return template;
    }

    @SuppressWarnings("lgtm[jenkins/unsafe-classes]")
    protected Script parseScript(Reader reader, String fileName) throws CompilationFailedException, IOException {
        String script = parse(reader);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("\n-- script source --");
            LOGGER.fine(script);
            LOGGER.fine("\n-- script end --\n");
        }
        try (GroovySandbox.Scope scope = new GroovySandbox().enter()) {
            return groovyShell.parse(script, fileName);
        } catch (Exception e) {
            throw new GroovyRuntimeException("Failed to parse template script (your template may contain an error or be trying to use expressions not currently supported): " + e, e);
        }
    }

    private static class SimpleTemplate implements Template {

        private final boolean sandbox;

        SimpleTemplate(boolean sandbox) {
            this.sandbox = sandbox;
        }

        protected Script script;

        public Writable make() {
            return make(null);
        }

        public Writable make(final Map map) {
            return new Writable() {
                /**
                 * Write the template document with the set binding applied to the writer.
                 *
                 * @see groovy.lang.Writable#writeTo(java.io.Writer)
                 */
                @Override public Writer writeTo(Writer writer) throws IOException {
                    Binding binding;
                    if (map == null)
                        binding = new Binding();
                    else
                        binding = new Binding(map);
                    PrintWriter pw = new PrintWriter(writer);
                    try {
                        if (sandbox) {
                            // Cannot use normal GroovySandbox.runScript here because template preparation was separated.
                            try (GroovySandbox.Scope scope = new GroovySandbox().enter()) {
                                final Script scriptObject = InvokerHelper.createScript(script.getClass(), binding);
                                scriptObject.setProperty("out", pw);
                                scriptObject.run();
                            }
                        } else {
                            final Script scriptObject = InvokerHelper.createScript(script.getClass(), binding);
                            scriptObject.setProperty("out", pw);
                            scriptObject.run();
                        }
                    } catch (MissingPropertyException x) {
                        throw (IOException) new IOException("did you forget to escape \\$" + x.getProperty() + " for non-Groovy variables?").initCause(x);
                    }
                    pw.flush();
                    return writer;
                }

                /**
                 * Convert the template and binding into a result String.
                 *
                 * @see java.lang.Object#toString()
                 */
                public String toString() {
                    StringWriter sw = new StringWriter();
                    try {
                        writeTo(sw);
                    } catch (IOException x) {
                        PrintWriter pw = new PrintWriter(sw);
                        x.printStackTrace(pw);
                        pw.flush();
                    }
                    return sw.toString();
                }
            };
        }
    }

    /**
     * Parse the text document looking for {@code <%} or {@code <%=} and then call out to the appropriate handler, otherwise copy the text directly
     * into the script while escaping quotes.
     *
     * @param reader a reader for the template text
     * @return the parsed text
     * @throws IOException if something goes wrong
     */
    protected String parse(Reader reader) throws IOException {
        if (!reader.markSupported()) {
            reader = new BufferedReader(reader);
        }
        StringWriter sw = new StringWriter();
        startScript(sw);
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '<') {
                reader.mark(1);
                c = reader.read();
                if (c != '%') {
                    sw.write('<');
                    reader.reset();
                } else {
                    reader.mark(1);
                    c = reader.read();
                    if (c == '=') {
                        groovyExpression(reader, sw);
                    } else {
                        reader.reset();
                        groovySection(reader, sw);
                    }
                }
                continue; // at least '<' is consumed ... read next chars.
            }
            if (c == '$') {
                reader.mark(1);
                c = reader.read();
                if (c != '{') {
                    sw.write('$');
                    reader.reset();
                } else {
                    reader.mark(1);
                    sw.write("${");
                    processGSstring(reader, sw);
                }
                continue; // at least '$' is consumed ... read next chars.
            }
            if (c == '\"') {
                sw.write('\\');
            }
            /*
             * Handle raw new line characters.
             */
            if (c == '\n' || c == '\r') {
                if (c == '\r') { // on Windows, "\r\n" is a new line.
                    reader.mark(1);
                    c = reader.read();
                    if (c != '\n') {
                        reader.reset();
                    }
                }
                sw.write("\n");
                continue;
            }
            sw.write(c);
        }
        endScript(sw);
        return sw.toString();
    }

    private void startScript(StringWriter sw) {
        sw.write("/* Generated by SimpleTemplateEngine */\n");
        sw.write(printMethod()+"\"\"\"");
    }

    private void endScript(StringWriter sw) {
        sw.write("\"\"\");\n");
    }

    private void processGSstring(Reader reader, StringWriter sw) throws IOException {
        int c;
        while ((c = reader.read()) != -1) {
            if (c != '\n' && c != '\r') {
                sw.write(c);
            }
            if (c == '}') {
                break;
            }
        }
    }

    /**
     * Closes the currently open write and writes out the following text as a GString expression until it reaches an end %>.
     *
     * @param reader a reader for the template text
     * @param sw     a StringWriter to write expression content
     * @throws IOException if something goes wrong
     */
    private void groovyExpression(Reader reader, StringWriter sw) throws IOException {
        sw.write("${");
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '%') {
                c = reader.read();
                if (c != '>') {
                    sw.write('%');
                } else {
                    break;
                }
            }
            if (c != '\n' && c != '\r') {
                sw.write(c);
            }
        }
        sw.write("}");
    }

    /**
     * Closes the currently open write and writes the following text as normal Groovy script code until it reaches an end %>.
     *
     * @param reader a reader for the template text
     * @param sw     a StringWriter to write expression content
     * @throws IOException if something goes wrong
     */
    private void groovySection(Reader reader, StringWriter sw) throws IOException {
        sw.write("\"\"\");");
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '%') {
                c = reader.read();
                if (c != '>') {
                    sw.write('%');
                } else {
                    break;
                }
            }
            /* Don't eat EOL chars in sections - as they are valid instruction separators.
             * See http://jira.codehaus.org/browse/GROOVY-980
             */
            // if (c != '\n' && c != '\r') {
            sw.write(c);
            //}
        }
        sw.write(";\n"+printMethod()+"\"\"\"");
    }

    protected String printMethod() {
        return "out.print(";
    }
}
