package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Content token that includes a file in the workspace.
 *
 * @author Kohsuke Kawaguchi
 */
public class WorkspaceFileContent implements EmailContent {
    public String getToken() {
        return "FILE";
    }

    public List<String> getArguments() {
        return Collections.singletonList(VAR_PATH_NAME);
    }

    public String getHelpText() {
        return "Includes the content of a specified file.\n" +
        "<ul>\n" +
        "<li><i>" + VAR_PATH_NAME + "</i> - The path to the file. Relative to the workspace root.\n" +
        "</ul>\n";
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>>
    String getContent(AbstractBuild<P, B> build, ExtendedEmailPublisher publisher, EmailType emailType, Map<String, ?> args) throws IOException, InterruptedException {
        String path = Args.get(args, VAR_PATH_NAME, null);
        if (path==null) throw new IllegalArgumentException("FILE token requires the "+VAR_PATH_NAME+" parameter");

        try {
            return build.getWorkspace().child(path).readToString();
        } catch (IOException e) {
            return null;    // this includes non-existent file, among others
        }
    }

    public boolean hasNestedContent() {
        return false;
    }

    public static final String VAR_PATH_NAME = "path";
}
