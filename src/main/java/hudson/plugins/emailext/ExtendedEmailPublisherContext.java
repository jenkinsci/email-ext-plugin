/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.emailext;

import com.google.common.collect.Multimap;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;

/**
 *
 * @author acearl
 */
public class ExtendedEmailPublisherContext {
    private ExtendedEmailPublisher publisher;
    private AbstractBuild<?, ?> build;
    private EmailTrigger trigger;
    private TaskListener listener;
    private Multimap<String, EmailTrigger> triggered;
    
    public ExtendedEmailPublisherContext(ExtendedEmailPublisher publisher, AbstractBuild<?, ?> build, TaskListener listener) {
        this.publisher = publisher;
        this.build = build;
        this.listener = listener;
    }
    
    public ExtendedEmailPublisher getPublisher() {
        return publisher;
    }
    
    protected void setPublisher(ExtendedEmailPublisher publisher) {
        this.publisher = publisher;
    }
    
    public AbstractBuild<?, ?> getBuild() {
        return build;
    }
    
    protected void setBuild(AbstractBuild<?, ?> build) {
        this.build = build;
    }
    
    public EmailTrigger getTrigger() {
        return trigger;
    }    
    
    protected void setTrigger(EmailTrigger trigger) {
        this.trigger = trigger;
    }
    
    public TaskListener getListener() {
        return listener;
    }
    
    protected void setListener(TaskListener listener) {
        this.listener = listener;
    }
    
    public Multimap<String, EmailTrigger> getTriggered() {
        return triggered;
    }
    
    protected void setTriggered(Multimap<String, EmailTrigger> triggered) {
        this.triggered = triggered;
    }
}
