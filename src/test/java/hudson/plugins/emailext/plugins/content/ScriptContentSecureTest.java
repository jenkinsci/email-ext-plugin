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

import hudson.model.Item;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        boolean allIsWell = false;
        try {
            super.templateInWorkspaceUnsafe();
        } catch (AssertionError e) {
            //As expected
            allIsWell = true;
        }
        assertTrue("Loading a template from the workspace should not be permitted.", allIsWell);
    }

    @Test
    public void testGroovyTemplateWithContentToken() throws Exception {
        super.testGroovyTemplateWithContentToken();
    }
}
