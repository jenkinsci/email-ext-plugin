/*
 * The MIT License
 *
 * Copyright (c) 2014 Stellar Science Ltd Co, K. R. Walker
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
package hudson.plugins.emailext.plugins.recipients;

import hudson.PluginManager;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    ExtendedEmailPublisherDescriptor.class,
    FreeStyleBuild.class,
    Jenkins.class,
    Mailer.class,
    Mailer.DescriptorImpl.class,
    User.class,
    FreeStyleProject.class,
    PluginManager.class
})
public class FailingTestSuspectsRecipientProviderTest {

    @Before
    public void before() throws Exception {
        final Jenkins jenkins = PowerMockito.mock(Jenkins.class);
        PowerMockito.when(jenkins.isUseSecurity()).thenReturn(false);
        final ExtendedEmailPublisherDescriptor extendedEmailPublisherDescriptor = PowerMockito.mock(ExtendedEmailPublisherDescriptor.class);
        extendedEmailPublisherDescriptor.setDebugMode(true);
        PowerMockito.when(extendedEmailPublisherDescriptor.getExcludedCommitters()).thenReturn("");

        PowerMockito.when(jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class)).thenReturn(extendedEmailPublisherDescriptor);

        final PluginManager pluginManager = PowerMockito.mock(PluginManager.class);
        Whitebox.setInternalState(pluginManager, "uberClassLoader", this.getClass().getClassLoader());

        PowerMockito.when(jenkins.getPluginManager()).thenReturn(pluginManager);

        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.doReturn(jenkins).when(Jenkins.class, "getActiveInstance");
        PowerMockito.doReturn(jenkins).when(Jenkins.class, "getInstance");

        final Mailer.DescriptorImpl descriptor = PowerMockito.mock(Mailer.DescriptorImpl.class);
        PowerMockito.when(descriptor.getDefaultSuffix()).thenReturn("DOMAIN");
        PowerMockito.mockStatic(Mailer.class);
        PowerMockito.doReturn(descriptor).when(Mailer.class, "descriptor");
    }

    @Test
    public void testAddRecipients() throws Exception {

        /*
         * Requestor: A
         * No committers.
         * Tests {a,b} fail.
         */
        final FreeStyleProject p = PowerMockito.mock(FreeStyleProject.class);
        final FreeStyleBuild build1 = PowerMockito.spy(new FreeStyleBuild(p));
        PowerMockito.doReturn(Result.UNSTABLE).when(build1).getResult();
        PowerMockito.doReturn(null).when(build1).getPreviousCompletedBuild();
        MockUtilities.addRequestor(build1, "A");
        MockUtilities.addTestResultAction(build1, build1, build1);
        TestUtilities.checkRecipients(build1, new FailingTestSuspectsRecipientProvider(), "A");

        /*
         * Requestor: (none)
         * Committers {U,V}.
         * Tests {a,b,c} fail.
         */
        final FreeStyleBuild build2 = PowerMockito.spy(new FreeStyleBuild(p));
        PowerMockito.doReturn(Result.UNSTABLE).when(build2).getResult();
        PowerMockito.doReturn(build1).when(build2).getPreviousCompletedBuild();
        MockUtilities.addChangeSet(build2, "U", "V");
        MockUtilities.addTestResultAction(build2, build1, build1, build2);
        TestUtilities.checkRecipients(build2, new FailingTestSuspectsRecipientProvider(), "A", "U", "V");

        /**
         * Requestor: (none)
         * Committers {X,V}.
         * Tests {c,d} fail.
         */
        final FreeStyleBuild build3 = PowerMockito.spy(new FreeStyleBuild(p));
        PowerMockito.doReturn(Result.UNSTABLE).when(build3).getResult();
        PowerMockito.doReturn(build2).when(build3).getPreviousCompletedBuild();
        MockUtilities.addChangeSet(build3, "X", "V");
        MockUtilities.addTestResultAction(build3, build2, build3);
        TestUtilities.checkRecipients(build3, new FailingTestSuspectsRecipientProvider(), "U", "V", "X");

        /**
         * Requestor: (none)
         * Committers {K}
         * No tests were performed. The build failed.
         */
        final FreeStyleBuild build4 = PowerMockito.spy(new FreeStyleBuild(p));
        PowerMockito.doReturn(Result.FAILURE).when(build4).getResult();
        PowerMockito.doReturn(build3).when(build4).getPreviousCompletedBuild();
        MockUtilities.addChangeSet(build4, "K");
        TestUtilities.checkRecipients(build4, new FailingTestSuspectsRecipientProvider());

        /**
         * Requestor: (none)
         * Committers {X,U,V}.
         * No tests were performed. The build failed.
         */
        final FreeStyleBuild build5 = PowerMockito.spy(new FreeStyleBuild(p));
        PowerMockito.doReturn(Result.FAILURE).when(build5).getResult();
        PowerMockito.doReturn(build4).when(build5).getPreviousCompletedBuild();
        MockUtilities.addChangeSet(build5, "U", "W");
        TestUtilities.checkRecipients(build5, new FailingTestSuspectsRecipientProvider());

        /**
         * Requestor: A
         * Committers {W}.
         * Tests {a,e (new test)} fail.
         */
        final FreeStyleBuild build6 = PowerMockito.spy(new FreeStyleBuild(p));
        PowerMockito.doReturn(Result.UNSTABLE).when(build6).getResult();
        PowerMockito.doReturn(build5).when(build6).getPreviousCompletedBuild();
        MockUtilities.addRequestor(build6, "A");
        MockUtilities.addChangeSet(build6, "W");
        MockUtilities.addTestResultAction(build6, build6, build6);
        TestUtilities.checkRecipients(build6, new FailingTestSuspectsRecipientProvider(), "A", "K", "U", "W");
    }
}
