/*
 * The MIT License
 *
 * Copyright 2015 acearl.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.emailext.plugins.content;

import hudson.FilePath;
import hudson.Plugin;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import jenkins.security.NotReallyRoleSensitiveCallable;
import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.scriptsecurity.scripts.Language;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 *
 * @author acearl
 */
public abstract class AbstractEvalContent extends DataBoundTokenMacro {
    
    protected static final String EMAIL_TEMPLATES_DIRECTORY = "email-templates";
    protected final String macroName;
    
    public AbstractEvalContent(String macroName) {
        this.macroName = macroName;
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build, build.getWorkspace(), listener, macroName);
    }

    @Override
    public abstract String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(this.macroName);
    }
    
    public static File scriptsFolder() {
        return new File(Jenkins.get().getRootDir(), EMAIL_TEMPLATES_DIRECTORY);
    }

    protected abstract Class<? extends ConfigProvider> getProviderClass();
    
    @Override
    public boolean hasNestedContent() {
        return false;
    }
    
    protected InputStream getFileInputStream(Run<?, ?> run, FilePath workspace, String fileName, String extension)
            throws IOException, InterruptedException {
        
        InputStream inputStream = null;
        if(fileName.startsWith("managed:")) {
            String managedFileName = fileName.substring(8);
            try {
                inputStream = getManagedFile(run, managedFileName);
            } catch(NoClassDefFoundError e) {
                inputStream = null;
            }
            
            if(inputStream == null) {
                throw new FileNotFoundException(String.format("Managed file '%s' not found", managedFileName));
            }
            return inputStream;
        }
        
        String fileExt = FilenameUtils.getExtension(fileName);
        
        // add default extension if needed
        if ("".equals(fileExt)) {
            fileName += extension;
        }    
        
        // next we look in the workspace, this means the filename is relative to the root of the workspace
        if(workspace != null) {
            FilePath file = workspace.child(fileName);
            if(file.exists() && isChildOf(file, workspace)) { //Guard against .. escapes
                inputStream = new UserProvidedContentInputStream(file.read());
            }
        }

        if(inputStream == null) {        
            inputStream = getClass().getClassLoader().getResourceAsStream(
                    "hudson/plugins/emailext/templates/" + fileName);

            if (inputStream == null) {
                File templateFile = new File(scriptsFolder(), fileName);

                // the file may have an extension, but not the correct one
                if(!templateFile.exists()) {
                    fileName += extension;
                    templateFile = new File(scriptsFolder(), fileName);
                }

                if (!templateFile.exists() || !isChildOf(new FilePath(templateFile), new FilePath(scriptsFolder()))) {
                    //guard against .. escapes
                    throw new FileNotFoundException(fileName); //Say whatever the user provided so we don't leak any information about the filesystem, but generateMissingFile should cover us.
                } else {
                    inputStream = new FileInputStream(templateFile);
                }
            }
        }

        return inputStream;
    }

    @Restricted(NoExternalUse.class)
    public static boolean isChildOf(final FilePath potentialChild, final FilePath parent) throws IOException, InterruptedException {
        //TODO JENKINS-26838 use API when available in core
        return parent.act(new IsChildFileCallable(potentialChild));
    }

    private InputStream getManagedFile(Run<?, ?> run, String fileName) {
        InputStream stream = null;
        Plugin plugin = Jenkins.get().getPlugin("config-file-provider");
        if (plugin != null) {
            Config config = null;
            List<Config> configs = ConfigFiles.getConfigsInContext(run.getParent().getParent(), getProviderClass());
            for (Config c : configs) {
                if (c.name.equalsIgnoreCase(fileName)) {
                    config = c;
                    break;
                }
            }

            if (config != null) {
               stream = new ByteArrayInputStream(config.content.getBytes(StandardCharsets.UTF_8));
            }
        }
        return stream;
    }
    
    protected String generateMissingFile(String type, String fileName) {
        return type + " file [" + fileName + "] was not found in $JENKINS_HOME/" + EMAIL_TEMPLATES_DIRECTORY + ".";
    }
    
    protected String getCharset(Run<?, ?> build) {
        return ExtendedEmailPublisher.descriptor().getCharset();
    }

    @Restricted(NoExternalUse.class)
    public static boolean isApprovedScript(final String script, final Language language) {
        final ScriptApproval approval = ScriptApproval.get();
        try {
            //checking doesn't check if we are system or not since it assumed being called from doCheckField
            try (ACLContext context = ACL.as2(Jenkins.ANONYMOUS2)) {
                return approval.checking(script, language).kind == FormValidation.Kind.OK;
            }
        } catch (Exception e) {
            Logger.getLogger(AbstractEvalContent.class.getName()).log(Level.WARNING, "Could not determine approval state of script.", e);
            return false;
        }
    }

    private static class IsChildFileCallable extends MasterToSlaveFileCallable<Boolean> {
        private final FilePath potentialChild;

        private IsChildFileCallable(FilePath potentialChild) {
            this.potentialChild = potentialChild;
        }

        @Override
        public Boolean invoke(File parent, VirtualChannel channel) {
            if (potentialChild.isRemote()) {
                //Not on the same machine so can't be a child of the local file
                return false;
            }
            FilePath test = potentialChild.getParent();
            FilePath target = new FilePath(parent);
            while(test != null && !target.equals(test)) {
                test = test.getParent();
            }
            return target.equals(test);
        }
    }
}
