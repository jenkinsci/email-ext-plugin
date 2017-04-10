package hudson.plugins.emailext.watching;

import hudson.Extension;
import hudson.model.*;
import hudson.model.Descriptor.FormException;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.tasks.Mailer;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author acearl
 */
public class EmailExtWatchAction implements Action {
    /**
     * Per user property that contains 
     */
    public static class UserProperty extends hudson.model.UserProperty {
        private List<EmailTrigger> triggers = new ArrayList<>();

        public UserProperty(List<EmailTrigger> triggers) {
            if(triggers != null) {
                this.triggers = Collections.unmodifiableList(triggers);
            }
        }

        @Exported
        public List<EmailTrigger> getTriggers() {
            return triggers;
        }
        
        private void clearTriggers() {
            triggers = Collections.emptyList();
        }

        @Extension
        public static final class DescriptorImpl extends UserPropertyDescriptor {

            public DescriptorImpl() {
                super(UserProperty.class);
            }

            public String getDisplayName() {
                return "Extended Email Job Watching";
            }

            @Override
            public UserProperty newInstance(User user) {
                return new UserProperty(null);
            }

            @Override
            public UserProperty newInstance(StaplerRequest req, JSONObject json) throws FormException {
                List<EmailTrigger> triggers = req != null ? req.bindJSONToList(EmailTrigger.class, json) : Collections.<EmailTrigger>emptyList();
                return new UserProperty(triggers);
            }
        }
    }
    
    private AbstractProject<?,?> project;
    
    public EmailExtWatchAction(AbstractProject project) {
        this.project = project;
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        if(isWatching()) {
            return hudson.plugins.emailext.Messages.EmailExtWatchAction_DisplayNameWatching();
        } else {
            return hudson.plugins.emailext.Messages.EmailExtWatchAction_DisplayName();
        }
    }

    public String getUrlName() {
        return "emailExtWatch";
    }   
    
    public AbstractProject<?, ?> getProject() {
        return project;
    }
    
    public boolean isWatching() {
        List<EmailTrigger> triggers = getTriggers();
        return triggers != null && !triggers.isEmpty();
    }
    
    public List<EmailTrigger> getTriggers() {
        List<EmailTrigger> triggers = null;
        User current = User.current();
        if(current != null) {
           UserProperty p = current.getProperty(UserProperty.class);
           if(p != null) {
               triggers = p.getTriggers();
           }
        }
        return triggers;
    }

    public EmailExtWatchJobProperty getJobProperty() throws IOException {
        EmailExtWatchJobProperty prop = project.getProperty(EmailExtWatchJobProperty.class);
        if(prop == null) {
            prop = new EmailExtWatchJobProperty();
            project.addProperty(prop);
        }
        return prop;
    }
    
    public Mailer.UserProperty getMailerProperty() {
        Mailer.UserProperty prop = null;
        User current = User.current();
        if(current != null) {
           prop = current.getProperty(Mailer.UserProperty.class);           
        }
        return prop;
    }
    
    public ExtendedEmailPublisher getPublisher() {
        ExtendedEmailPublisher p = null;
        for(Publisher pub : project.getPublishersList()) {
            if(pub instanceof ExtendedEmailPublisher) {
                p = (ExtendedEmailPublisher)pub;
            }
        }
        return p;
    }
    
    public void doStopWatching(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        User user = User.current();
        if(user != null) {
            stopWatching();
            for(hudson.model.UserProperty property : user.getAllProperties()) {
                if(property instanceof EmailExtWatchAction.UserProperty) {
                    ((EmailExtWatchAction.UserProperty)property).clearTriggers();
                    break;
                }
            }            
        }    
        rsp.sendRedirect(project.getAbsoluteUrl());
    }
    
    @RequirePOST
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        User user = User.current();
        if(user != null) {
            Object json = req.getSubmittedForm().get("triggers");
            List<EmailTrigger> triggers = req.bindJSONToList(EmailTrigger.class, json);

            List<EmailTrigger> unwatchable = new ArrayList<>();
            for(EmailTrigger trigger : triggers) {
                if(!trigger.getDescriptor().isWatchable()) {
                    unwatchable.add(trigger);
                }
            }

            triggers.removeAll(unwatchable);

            Mailer.UserProperty mailerProperty = getMailerProperty();
            
            if(mailerProperty != null) {
                // override so that the emails only get sent to them.
                for(EmailTrigger trigger : triggers) {
                    trigger.getEmail().setRecipientList(mailerProperty.getAddress());
                    trigger.getEmail().getRecipientProviders().clear();
                }

                startWatching();
                user.addProperty(new UserProperty(triggers));
            }
        }
        rsp.sendRedirect(project.getAbsoluteUrl());
    }
    
    public void startWatching() throws IOException {
        User user = User.current();
        if(user != null) {
            getJobProperty().addWatcher(user);
            project.save();
        }
    }
    
    public void stopWatching() throws IOException {
        User user = User.current();
        if (user != null) {
            getJobProperty().removeWatcher(user);
            project.save();
        }
    }
    
    public boolean isWatching(User user) throws IOException {
        return getJobProperty().isWatching(user);
    }
}
