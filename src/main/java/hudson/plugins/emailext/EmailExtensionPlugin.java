package hudson.plugins.emailext;

import hudson.Plugin;
import hudson.plugins.emailext.plugins.EmailContent;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.content.BuildLogContent;
import hudson.plugins.emailext.plugins.content.BuildNumberContent;
import hudson.plugins.emailext.plugins.content.BuildStatusContent;
import hudson.plugins.emailext.plugins.content.BuildURLContent;
import hudson.plugins.emailext.plugins.content.ChangesSinceLastBuildContent;
import hudson.plugins.emailext.plugins.content.ChangesSinceLastSuccessfulBuildContent;
import hudson.plugins.emailext.plugins.content.HudsonURLContent;
import hudson.plugins.emailext.plugins.content.ProjectNameContent;
import hudson.plugins.emailext.plugins.content.ProjectURLContent;
import hudson.plugins.emailext.plugins.trigger.FailureTrigger;
import hudson.plugins.emailext.plugins.trigger.FixedTrigger;
import hudson.plugins.emailext.plugins.trigger.StillFailingTrigger;
import hudson.plugins.emailext.plugins.trigger.StillUnstableTrigger;
import hudson.plugins.emailext.plugins.trigger.SuccessTrigger;
import hudson.plugins.emailext.plugins.trigger.UnstableTrigger;
import hudson.tasks.BuildStep;

/**
 * Entry point of a plugin.
 *
 * <p>
 * There must be one {@link Plugin} class in each plugin.
 * See javadoc of {@link Plugin} for more about what can be done on this class.
 *
 * @author kyle.sweeney@valtech.com
 * @plugin
 */
public class EmailExtensionPlugin extends Plugin {
    public void start() throws Exception {
        // plugins normally extend Hudson by providing custom implementations
        // of 'extension points'. In this case, we are adding the EmailExtension plugin 
    	// to the list of publishers.
   	
    	BuildStep.PUBLISHERS.add(ExtendedEmailPublisher.DESCRIPTOR);
    	
    	//We are also adding different Content plugins to the list of content types.
    	addEmailContentPlugin(new BuildNumberContent());
    	addEmailContentPlugin(new BuildStatusContent());
    	addEmailContentPlugin(new BuildURLContent());
    	addEmailContentPlugin(new ChangesSinceLastBuildContent());
    	addEmailContentPlugin(new ChangesSinceLastSuccessfulBuildContent());
    	addEmailContentPlugin(new HudsonURLContent());
    	addEmailContentPlugin(new ProjectNameContent());
    	addEmailContentPlugin(new ProjectURLContent());
        addEmailContentPlugin(new BuildLogContent());
    	
    	addEmailTriggerPlugin(FailureTrigger.DESCRIPTOR);
    	addEmailTriggerPlugin(StillFailingTrigger.DESCRIPTOR);
    	addEmailTriggerPlugin(UnstableTrigger.DESCRIPTOR);
    	addEmailTriggerPlugin(StillUnstableTrigger.DESCRIPTOR);
    	addEmailTriggerPlugin(SuccessTrigger.DESCRIPTOR);
    	addEmailTriggerPlugin(FixedTrigger.DESCRIPTOR);
    }
    
    private void addEmailContentPlugin(EmailContent content){
    	try{
    		ExtendedEmailPublisher.addEmailContentType(content);
    	}
    	catch (EmailExtException e){
    		System.out.println("Content type " + content + " was already added.");
    	}
    }
    
    private void addEmailTriggerPlugin(EmailTriggerDescriptor trigger){
    	try{
    		ExtendedEmailPublisher.addEmailTriggerType(trigger);
    	}
    	catch (EmailExtException e){
    		System.out.println("Trigger type " + trigger.getTriggerName() + " was already added.");
    	}
    }
}
