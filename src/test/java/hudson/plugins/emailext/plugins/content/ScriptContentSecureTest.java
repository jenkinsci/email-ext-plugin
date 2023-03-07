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

package hudson.plugins.emailext.plugins.content;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.GroovyTemplateConfig;
import hudson.util.LogTaskListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.configfiles.folder.FolderConfigFileAction;
import org.jenkinsci.plugins.configfiles.folder.FolderConfigFileProperty;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

/**
 * Runs some {@link ScriptContentTest} in a secured Jenkins.
 */
public class ScriptContentSecureTest extends ScriptContentTest {

    @Before
    public void setup() {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                                                   .grant(Jenkins.ADMINISTER).everywhere().to("bob")
                                                   .grant(Jenkins.READ, Item.READ, Item.EXTENDED_READ).everywhere().to("alice"));
    }

    @Test
    public void testShouldFindScriptOnClassPath() throws Exception {
        super.testShouldFindScriptOnClassPath();
    }

    @Test
    public void testShouldFindTemplateOnClassPath() throws Exception {
        super.testShouldFindTemplateOnClassPath();
    }

    @Test
    public void templateInWorkspace() throws Exception {
        ScriptApproval.get().clearApprovedScripts();
        super.templateInWorkspace();
    }

    @Test
    public void templateInWorkspaceUnsafe() throws Exception {
        ScriptApproval.get().clearApprovedScripts();
        assertThrows(
                "Templates in the workspace should use the Groovy sandbox",
                AssertionError.class,
                super::templateInWorkspaceUnsafe);
        // Error message is a TokenMacro parse error and does not contain a script security warning,
        // perhaps due to EmailExtScript#methodMissing? The message looks like:
        // Error processing tokens: Error while parsing action 'Text/ZeroOrMore/FirstOf/Token/DelimitedToken/DelimitedToken_Action3' at input position (line 1, pos 39)
        assertNull(j.jenkins.getItem("should-not-exist"));
    }

    @Issue("SECURITY-1340")
    @Test
    public void templateInWorkspaceWithConstructor() throws Exception {
        ScriptApproval.get().clearApprovedScripts();
        AssertionError e =
                assertThrows(
                        "Templates in the workspace should use the Groovy sandbox",
                        AssertionError.class,
                        () -> super.templateInWorkspace("/testConstructor.groovy"));
        assertThat(
                e.getMessage(), containsString("staticMethod jenkins.model.Jenkins getInstance"));
        assertNull(j.jenkins.getItem("should-not-exist"));
    }

    @Test
    public void testGroovyTemplateWithContentToken() throws Exception {
        super.testGroovyTemplateWithContentToken();
    }

    @Test @Issue("SECURITY-2939")
    public void managedBadTemplateInFolder() throws Exception {
        final Folder folder = j.createProject(Folder.class, "sub");

        final FreeStyleProject project = folder.createProject(FreeStyleProject.class, "Free");
        project.getBuildersList().add(new CaptureEnvironmentBuilder());
        project.getPublishersList().add(new ExtendedEmailPublisher());
        final FreeStyleBuild build = j.buildAndAssertSuccess(project);
        FolderConfigFileAction folderConfigFileAction = folder.getAction(FolderConfigFileAction.class);
        folderConfigFileAction.getGroupedConfigs(); //Should create the config store accessed below
        final FolderConfigFileProperty folderConfigFileProperty = folder.getProperties().get(FolderConfigFileProperty.class);
        folderConfigFileProperty.getConfigs().add(
                new GroovyTemplateConfig(
                "long-long-id", "test.groovy", "Bad groovy template script",
                        "<%\n" +
                                "  // Whatever Groovy code you want\n" +
                                "  Jenkins.get().setSystemMessage(\"You got hax0red\");\n" +
                                "  // You can easily exfiltrate data using out.print or throwing an exception with the data in the message\n" +
                                "  out.println(Jenkins.get().getSystemMessage());\n" +
                                "%>")
        );
        ScriptContent scriptContent = new ScriptContent();
        scriptContent.template = "managed:test.groovy";
        final LogTaskListener listener = new LogTaskListener(Logger.getLogger(getClass().getName()), Level.INFO);
        String result = scriptContent.evaluate(build, listener, "SCRIPT");
        assertThat(result, not(containsString("You got hax0red")));
        assertNotEquals("You got hax0red", Jenkins.get().getSystemMessage());
        assertThat(result, containsString("Scripts not permitted to use"));

        final ScriptApproval scriptApproval = ScriptApproval.get();
        assertThat(scriptApproval.getPendingScripts(), hasSize(1));
        scriptApproval.preapproveAll();

        scriptContent = new ScriptContent();
        scriptContent.template = "managed:test.groovy";
        result = scriptContent.evaluate(build, listener, "SCRIPT");
        assertThat(result, containsString("You got hax0red"));
        assertEquals("You got hax0red", Jenkins.get().getSystemMessage());
        Jenkins.get().setSystemMessage("");

        scriptApproval.clearApprovedScripts();

        scriptContent = new ScriptContent();
        scriptContent.template = "managed:test.groovy";
        result = scriptContent.evaluate(build, listener, "SCRIPT");
        assertThat(result, not(containsString("You got hax0red")));
        assertNotEquals("You got hax0red", Jenkins.get().getSystemMessage());
        assertThat(result, containsString("Scripts not permitted to use"));
    }
}
