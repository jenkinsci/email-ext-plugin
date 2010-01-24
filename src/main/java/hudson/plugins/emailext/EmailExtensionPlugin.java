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
		addEmailContentPlugin(new BuildNumberContent());
		addEmailContentPlugin(new BuildStatusContent());
		addEmailContentPlugin(new BuildURLContent());
		addEmailContentPlugin(new ChangesSinceLastBuildContent());
		addEmailContentPlugin(new ChangesSinceLastSuccessfulBuildContent());
		addEmailContentPlugin(new EnvContent());
		addEmailContentPlugin(new FailedTestsContent());
		addEmailContentPlugin(new HudsonURLContent());
		addEmailContentPlugin(new ProjectNameContent());
		addEmailContentPlugin(new ProjectURLContent());
		addEmailContentPlugin(new SVNRevisionContent());
        addEmailContentPlugin(new CauseContent());
		
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
