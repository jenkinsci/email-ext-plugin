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
    private Run<?, ?> run;
    private FilePath workspace;
    private EmailTrigger trigger;
    private TaskListener listener;
    private Launcher launcher;
    private Multimap<String, EmailTrigger> triggered;

    @Deprecated
    public ExtendedEmailPublisherContext(ExtendedEmailPublisher publisher, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        this(publisher, build, build.getWorkspace(), launcher, listener);
    }

    public ExtendedEmailPublisherContext(ExtendedEmailPublisher publisher, Run<?,?> run, FilePath workspace, Launcher launcher, TaskListener listener) {
        this.publisher = publisher;
        this.run = run;
        this.workspace = workspace;
        this.launcher = launcher;
        this.listener = listener;
    }
    
    public ExtendedEmailPublisher getPublisher() {
        return publisher;
    }
    
    protected void setPublisher(ExtendedEmailPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * @see ExtendedEmailPublisherContext#getRun()
     */
    @Deprecated
    public AbstractBuild<?, ?> getBuild() {
        if(run instanceof AbstractBuild) {
            return (AbstractBuild<?, ?>)run;
        }
        return null;
    }

    public Run<?,?> getRun() {
        return run;
    }

    public FilePath getWorkspace() {
        return workspace;
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
