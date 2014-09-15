package hudson.plugins.emailext;

import com.google.common.collect.Multimap;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.EmailTrigger;

/**
 *
 * @author acearl
 */
public class ExtendedEmailPublisherContext {
    private ExtendedEmailPublisher publisher;
    private FilePath workspace;
    private Run<?, ?> build;
    private EmailTrigger trigger;
    private TaskListener listener;
    private Launcher launcher;
    private Multimap<String, EmailTrigger> triggered;

    public ExtendedEmailPublisherContext(ExtendedEmailPublisher publisher, AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) {
        this(publisher, build.getWorkspace(), build, launcher, listener);
    }

    public ExtendedEmailPublisherContext(ExtendedEmailPublisher publisher, FilePath workspace, Run<?, ?> build, Launcher launcher, TaskListener listener) {
        this.publisher = publisher;
        this.workspace = workspace;
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    public ExtendedEmailPublisher getPublisher() {
        return publisher;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    protected void setPublisher(ExtendedEmailPublisher publisher) {
        this.publisher = publisher;
    }
    
    public Run<?, ?> getBuild() {
        return build;
    }
    
    protected void setBuild(Run<?, ?> build) {
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
