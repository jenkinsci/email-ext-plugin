/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author acearl
 */
@Extension
public class EmailExtTemplateActionFactory extends TransientProjectActionFactory {

    @Override
    public Collection<? extends Action> createFor(AbstractProject target) {
        boolean hasEmailExt = false;
        if (target.getPublishersList() != null) {
            for(Object p : target.getPublishersList()) {
                if(p instanceof ExtendedEmailPublisher) {
                    hasEmailExt = true;
                    break;
                }
            }
        }        
        
        if(hasEmailExt) {
            return Collections.singletonList(new EmailExtTemplateAction(target));
        }
        return Collections.emptyList();
    }        
}
