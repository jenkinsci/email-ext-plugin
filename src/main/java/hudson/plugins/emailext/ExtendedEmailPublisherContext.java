package hudson.plugins.emailext;

import com.google.common.collect.Multimap;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.AbstractEmailTrigger;

/**
 *
 * @author acearl
 */
public class ExtendedEmailPublisherContext {
    private ExtendedEmailPublisher publisher;
    private Run<?, ?> run;
    private FilePath workspace;
    private AbstractEmailTrigger trigger;
    private TaskListener listener;
    private Launcher launcher;
    private Multimap<String, AbstractEmailTrigger> triggered;

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

    @Deprecated
    /**
     * @see ExtendedEmailPublisherContext#getRun()
     */
    public AbstractBuild<?, ?> getBuild() {
        if(run instanceof AbstractBuild) {
            return (AbstractBuild)run;
        }
        return null;
    }

    public Run<?,?> getRun() {
        return run;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public AbstractEmailTrigger getTrigger() {
        return trigger;
    }    
    
    protected void setTrigger(AbstractEmailTrigger trigger) {
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
    
    public Multimap<String, AbstractEmailTrigger> getTriggered() {
        return triggered;
    }
    
    protected void setTriggered(Multimap<String, AbstractEmailTrigger> triggered) {
        this.triggered = triggered;
    }
}
