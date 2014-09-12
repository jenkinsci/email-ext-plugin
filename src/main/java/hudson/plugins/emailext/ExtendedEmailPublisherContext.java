package hudson.plugins.emailext;

import com.google.common.collect.Multimap;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.emailext.plugins.EmailTrigger;

/**
 *
 * @author acearl
 */
public class ExtendedEmailPublisherContext {
    private ExtendedEmailPublisher publisher;
    private AbstractBuild<?, ?> build;
    private EmailTrigger trigger;
    private BuildListener listener;
    private Launcher launcher;
    private Multimap<String, EmailTrigger> triggered;
    
    public ExtendedEmailPublisherContext(ExtendedEmailPublisher publisher, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        this.publisher = publisher;
        this.build = build;
        this.launcher = launcher;
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
    
    protected void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }
    
    public Launcher getLauncher() {
        return launcher;
    }
    
    public BuildListener getListener() {
        return listener;
    }
    
    protected void setListener(BuildListener listener) {
        this.listener = listener;
    }
    
    public Multimap<String, EmailTrigger> getTriggered() {
        return triggered;
    }
    
    protected void setTriggered(Multimap<String, EmailTrigger> triggered) {
        this.triggered = triggered;
    }
}
