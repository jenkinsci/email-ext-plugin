/*
 * The MIT License
 * 
 * Copyright (c) 2010, kyle.sweeney@valtech.com, Stellar Science Ltd Co, K. R. Walker
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
package hudson.plugins.emailext;

import hudson.Plugin;
import hudson.plugins.emailext.plugins.ContentBuilder;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.content.BuildIdContent;
import hudson.plugins.emailext.plugins.content.BuildLogContent;
import hudson.plugins.emailext.plugins.content.BuildLogRegexContent;
import hudson.plugins.emailext.plugins.content.BuildLogExcerptContent;
import hudson.plugins.emailext.plugins.content.BuildNumberContent;
import hudson.plugins.emailext.plugins.content.BuildStatusContent;
import hudson.plugins.emailext.plugins.content.BuildURLContent;
import hudson.plugins.emailext.plugins.content.CauseContent;
import hudson.plugins.emailext.plugins.content.ChangesSinceLastBuildContent;
import hudson.plugins.emailext.plugins.content.ChangesSinceLastSuccessfulBuildContent;
import hudson.plugins.emailext.plugins.content.ChangesSinceLastUnstableBuildContent;
import hudson.plugins.emailext.plugins.content.EnvContent;
import hudson.plugins.emailext.plugins.content.FailedTestsContent;
import hudson.plugins.emailext.plugins.content.HudsonURLContent;
import hudson.plugins.emailext.plugins.content.JellyScriptContent;
import hudson.plugins.emailext.plugins.content.JenkinsURLContent;
import hudson.plugins.emailext.plugins.content.JobDescriptionContent;
import hudson.plugins.emailext.plugins.content.ProjectNameContent;
import hudson.plugins.emailext.plugins.content.ProjectURLContent;
import hudson.plugins.emailext.plugins.content.SVNRevisionContent;
import hudson.plugins.emailext.plugins.content.ScriptContent;
import hudson.plugins.emailext.plugins.content.TestCountsContent;
import hudson.plugins.emailext.plugins.content.WorkspaceFileContent;
import hudson.plugins.emailext.plugins.trigger.AbortedTrigger;
import hudson.plugins.emailext.plugins.trigger.FailureTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedTrigger;
import hudson.plugins.emailext.plugins.trigger.ImprovementTrigger;
import hudson.plugins.emailext.plugins.trigger.NotBuiltTrigger;
import hudson.plugins.emailext.plugins.trigger.PreBuildTrigger;
import hudson.plugins.emailext.plugins.trigger.RegressionTrigger;
import hudson.plugins.emailext.plugins.trigger.StillFailingTrigger;
import hudson.plugins.emailext.plugins.trigger.StillUnstableTrigger;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import hudson.plugins.emailext.plugins.trigger.UnstableTrigger;

import java.util.Arrays;

/**
 * Entry point of a plugin.
 *
 * <p>
 * There must be one {@link Plugin} class in each plugin.
 * See javadoc of {@link Plugin} for more about what can be done on this class.
 *
 * @author kyle.sweeney@valtech.com
 */
public class EmailExtensionPlugin extends Plugin {
    @Override
    public void start() throws Exception {
        //We are adding different Content plugins to the list of content types.
        addEmailContentPlugin(new BuildIdContent());
        addEmailContentPlugin(new BuildLogContent());
        addEmailContentPlugin(new BuildLogRegexContent());
        addEmailContentPlugin(new BuildLogExcerptContent());
        addEmailContentPlugin(new BuildNumberContent());
        addEmailContentPlugin(new BuildStatusContent());
        addEmailContentPlugin(new BuildURLContent());
        addEmailContentPlugin(new ChangesSinceLastBuildContent());
        addEmailContentPlugin(new ChangesSinceLastSuccessfulBuildContent());
        addEmailContentPlugin(new ChangesSinceLastUnstableBuildContent());
        addEmailContentPlugin(new EnvContent());
        addEmailContentPlugin(new FailedTestsContent());
        addEmailContentPlugin(new JenkinsURLContent());
        addEmailContentPlugin(new HudsonURLContent());
        addEmailContentPlugin(new ProjectNameContent());
        addEmailContentPlugin(new ProjectURLContent());
        addEmailContentPlugin(new SVNRevisionContent());
        addEmailContentPlugin(new CauseContent());
        addEmailContentPlugin(new JellyScriptContent());
        addEmailContentPlugin(new WorkspaceFileContent());
        addEmailContentPlugin(new TestCountsContent());
        addEmailContentPlugin(new JobDescriptionContent());
        try {
            addEmailContentPlugin(new ScriptContent());
        } catch (NoClassDefFoundError jvm15Error) {
            System.err.println( "ScriptContent not available for JVM 1.5" );
        }

        addEmailTriggerPlugin(PreBuildTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(FailureTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(StillFailingTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(UnstableTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(StillUnstableTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(SuccessTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(FixedTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(AbortedTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(NotBuiltTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(ImprovementTrigger.DESCRIPTOR);
        addEmailTriggerPlugin(RegressionTrigger.DESCRIPTOR);
    }

    private void addEmailContentPlugin(EmailContent content) {
        try {
            ContentBuilder.addEmailContentType(content);
        } catch (EmailExtException e) {
            System.err.println(e.getMessage());
        }
    }

    private void addEmailTriggerPlugin(EmailTriggerDescriptor trigger) {
        try {
            ExtendedEmailPublisher.addEmailTriggerType(trigger);
        } catch (EmailExtException e) {
            System.err.println(e.getMessage());
        }
    }

    static {
        // Fix JENKINS-9006
        // When sending to multiple recipients, send to valid recipients even if some are
        // invalid, unless we have explicitly said otherwise.

        // we need this here as well as in the MailerTask because its possible we never actually
        // use the MailerTask, which would mean it's static would never be called.
        for (String property: Arrays.asList("mail.smtp.sendpartial", "mail.smtps.sendpartial")) {
            if (System.getProperty(property) == null) {
                System.setProperty(property, "true");
            }
        }
    }
}
