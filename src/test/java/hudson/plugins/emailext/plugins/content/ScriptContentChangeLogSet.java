package hudson.plugins.emailext.plugins.content;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by acearl on 12/3/2014.
 */
public class ScriptContentChangeLogSet extends ChangeLogSet<ChangeLogSet.Entry> {

    public ScriptContentChangeLogSet(AbstractBuild build) {
        super(build, null);
    }

    @Override
    public boolean isEmptySet() {
        return false;
    }

    public Iterator iterator() {
        return Collections.singletonList(new Entry() {
            @Override
            public String getMsg() {
                return "COMMIT MESSAGE";
            }

            @Override
            public User getAuthor() {
                User user = mock(User.class);
                when(user.getDisplayName()).thenReturn("Kohsuke Kawaguchi");
                when(user.getFullName()).thenReturn("Kohsuke Kawaguchi");
                return user;
            }

            @Override
            public Collection<String> getAffectedPaths() {
                return Arrays.asList("path1", "path2");
            }

            @Override
            public String getMsgAnnotated() {
                return getMsg();
            }

            @Override
            public Collection<? extends AffectedFile> getAffectedFiles() {
                return Arrays.asList(
                        new AffectedFile() {
                            public String getPath() {
                                return "path1";
                            }

                            public EditType getEditType() {
                                return EditType.EDIT;
                            }
                        },
                        new AffectedFile() {
                            public String getPath() {
                                return "path2";
                            }

                            public EditType getEditType() {
                                return EditType.ADD;
                            }
                        });
            }
        }).iterator();
    }
}
