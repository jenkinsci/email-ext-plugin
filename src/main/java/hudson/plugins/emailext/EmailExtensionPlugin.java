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
import hudson.plugins.emailext.plugins.content.*;
import hudson.plugins.emailext.plugins.trigger.*;

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
		addEmailContentPlugin(new BuildLogContent());
		addEmailContentPlugin(new BuildLogRegexContent());
		addEmailContentPlugin(new BuildNumberContent());
		addEmailContentPlugin(new BuildStatusContent());
		addEmailContentPlugin(new BuildURLContent());
		addEmailContentPlugin(new ChangesSinceLastBuildContent());
		addEmailContentPlugin(new ChangesSinceLastSuccessfulBuildContent());
		addEmailContentPlugin(new ChangesSinceLastUnstableBuildContent());
		addEmailContentPlugin(new EnvContent());
		addEmailContentPlugin(new FailedTestsContent());
		addEmailContentPlugin(new HudsonURLContent());
		addEmailContentPlugin(new ProjectNameContent());
		addEmailContentPlugin(new ProjectURLContent());
		addEmailContentPlugin(new SVNRevisionContent());
        addEmailContentPlugin(new CauseContent());
        addEmailContentPlugin(new JellyScriptContent());

		addEmailTriggerPlugin(PreBuildTrigger.DESCRIPTOR);
		addEmailTriggerPlugin(FailureTrigger.DESCRIPTOR);
		addEmailTriggerPlugin(StillFailingTrigger.DESCRIPTOR);
		addEmailTriggerPlugin(UnstableTrigger.DESCRIPTOR);
		addEmailTriggerPlugin(StillUnstableTrigger.DESCRIPTOR);
		addEmailTriggerPlugin(SuccessTrigger.DESCRIPTOR);
		addEmailTriggerPlugin(FixedTrigger.DESCRIPTOR);
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

}
