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

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class FirstFailingBuildSuspectsRecipientProviderTest {

    private MockedStatic<Jenkins> mockedJenkins;
    private MockedStatic<Mailer> mockedMailer;

    @Before
    public void before() {
        final Jenkins jenkins = Mockito.mock(Jenkins.class);
        Mockito.when(jenkins.isUseSecurity()).thenReturn(false);
        final ExtendedEmailPublisherDescriptor extendedEmailPublisherDescriptor = Mockito.mock(ExtendedEmailPublisherDescriptor.class);
        extendedEmailPublisherDescriptor.setDebugMode(true);

        Mockito.when(jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class)).thenReturn(extendedEmailPublisherDescriptor);

        mockedJenkins = Mockito.mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);

        final Mailer.DescriptorImpl descriptor = Mockito.mock(Mailer.DescriptorImpl.class);
        Mockito.when(descriptor.getDefaultSuffix()).thenReturn("DOMAIN");
        mockedMailer = Mockito.mockStatic(Mailer.class);
        mockedMailer.when(Mailer::descriptor).thenReturn(descriptor);
    }

    @After
    public void after() {
        mockedMailer.close();
        mockedJenkins.close();
    }

    @Test
    public void testAddRecipients() throws Exception {

        /*
         * Requestor: A
         * No committers.
         * Failed.
         */
        try (MockedStatic<User> mockedUser = Mockito.mockStatic(User.class)) {
            final FreeStyleProject p = Mockito.mock(FreeStyleProject.class);
            final FreeStyleBuild build1 = Mockito.spy(new FreeStyleBuild(p));
            Mockito.doReturn(Result.FAILURE).when(build1).getResult();
            Mockito.doReturn(null).when(build1).getPreviousCompletedBuild();
            MockUtilities.addRequestor(mockedUser, build1, "A");
            TestUtilities.checkRecipients(build1, new FirstFailingBuildSuspectsRecipientProvider(), "A");

            /*
             * Requestor: A
             * No committers.
             * Unstable.
             */
            final FreeStyleBuild build2 = Mockito.spy(new FreeStyleBuild(p));
            Mockito.doReturn(Result.UNSTABLE).when(build2).getResult();
            Mockito.doReturn(build1).when(build2).getPreviousCompletedBuild();
            MockUtilities.addRequestor(mockedUser, build2, "A");
            TestUtilities.checkRecipients(build2, new FirstFailingBuildSuspectsRecipientProvider());

            /*
             * Requestor: A
             * Committers {X,V}.
             * Failed.
             */
            final FreeStyleBuild build3 = Mockito.spy(new FreeStyleBuild(p));
            Mockito.doReturn(Result.FAILURE).when(build3).getResult();
            Mockito.doReturn(build2).when(build3).getPreviousCompletedBuild();
            MockUtilities.addRequestor(mockedUser, build3, "A");
            MockUtilities.addChangeSet(build3, "X", "V");
            TestUtilities.checkRecipients(build3, new FirstFailingBuildSuspectsRecipientProvider(), "X", "V", "A");

            /*
             * Requestor: B
             * Committers {X}.
             * Aborted.
             */
            final FreeStyleBuild build4 = Mockito.spy(new FreeStyleBuild(p));
            Mockito.doReturn(Result.ABORTED).when(build4).getResult();
            Mockito.doReturn(build3).when(build4).getPreviousCompletedBuild();
            MockUtilities.addRequestor(mockedUser, build4, "B");
            MockUtilities.addChangeSet(build4, "X");
            TestUtilities.checkRecipients(build4, new FirstFailingBuildSuspectsRecipientProvider());

            /*
             * Requestor: B
             * Committers {U,V}.
             * Failed.
             */
            final FreeStyleBuild build5 = Mockito.spy(new FreeStyleBuild(p));
            Mockito.doReturn(Result.FAILURE).when(build5).getResult();
            Mockito.doReturn(build4).when(build5).getPreviousCompletedBuild();
            MockUtilities.addRequestor(mockedUser, build5, "B");
            MockUtilities.addChangeSet(build5, "U", "V");
            TestUtilities.checkRecipients(build5, new FirstFailingBuildSuspectsRecipientProvider(), "X", "V", "A");

            /*
             * Requestor: A
             * Committers {W}.
             * Success.
             */
            final FreeStyleBuild build6 = Mockito.spy(new FreeStyleBuild(p));
            Mockito.doReturn(Result.UNSTABLE).when(build6).getResult();
            Mockito.doReturn(build5).when(build6).getPreviousCompletedBuild();
            MockUtilities.addRequestor(mockedUser, build6, "A");
            MockUtilities.addChangeSet(build6, "W");
            TestUtilities.checkRecipients(build6, new FirstFailingBuildSuspectsRecipientProvider());
        }
    }
}
