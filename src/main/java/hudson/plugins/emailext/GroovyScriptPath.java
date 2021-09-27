package hudson.plugins.emailext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Model a classpath entry for Groovy pre script execution.
 * The syntax can be an URL, and if the syntax is invalid, defaults to a
 * file path.
 * This has been inspired by the Jenkins Postbuild plugin.
 * 
 * @see <a href="https://github.com/jenkinsci/groovy-postbuild-plugin">https://github.com/jenkinsci/groovy-postbuild-plugin</a>
 * @see <a href="https://github.com/jenkinsci/groovy-postbuild-plugin/blob/master/src/main/java/org/jvnet/hudson/plugins/groovypostbuild/GroovyPostbuildRecorder.java">https://github.com/jenkinsci/groovy-postbuild-plugin/blob/master/src/main/java/org/jvnet/hudson/plugins/groovypostbuild/GroovyPostbuildRecorder.java</a>
 * 
 */
public class GroovyScriptPath extends AbstractDescribableImpl<GroovyScriptPath> {

    private String path;
    
    @DataBoundConstructor
    public GroovyScriptPath(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }

    public URL asURL() {
        URL url = null;
        
        try {
            url = new URL(path);
        }
        catch (MalformedURLException e) {
            try {
                url = new File(path).toURI().toURL();
            } catch (MalformedURLException e1) {
            }
        }
        return url;
    }
    
    @Extension
    public static class GroovyScriptPathDescriptor extends Descriptor<GroovyScriptPath> {
        @NonNull
        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
