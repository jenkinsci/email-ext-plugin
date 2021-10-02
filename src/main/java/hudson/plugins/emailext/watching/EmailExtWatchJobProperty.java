package hudson.plugins.emailext.watching;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Created by acearl on 12/4/2014.
 */
public class EmailExtWatchJobProperty extends JobProperty<Job<?, ?>> {

    final List<String> watchers = new ArrayList<>();

    @NonNull
    @Override
    public Collection<Action> getJobActions(Job<?, ?> job) {
        return Collections.emptyList();
    }

    public List<String> getWatchers() {
        return Collections.unmodifiableList(watchers);
    }

    public void addWatcher(User user) {
        String existing = null;
        for (String u : watchers) {
            if (u.compareTo(user.getId()) == 0) {
                existing = u;
                break;
            }
        }

        if (existing == null) {
            watchers.add(user.getId());
        }
    }

    public void removeWatcher(User user) {
        String remove = null;
        for (String u : watchers) {
            if (u.compareTo(user.getId()) == 0) {
                remove = u;
                break;
            }
        }

        if (remove != null) {
            watchers.remove(user.getId());
        }
    }

    public boolean isWatching(User user) {
        for(String u : watchers) {
            if(u.compareTo(user.getId()) == 0) {
                return true;
            }
        }
        return false;
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req,
                                          JSONObject formData) throws FormException {
            return null;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
