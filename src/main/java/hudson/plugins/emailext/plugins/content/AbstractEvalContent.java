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

import hudson.Plugin;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import jenkins.model.Jenkins;
import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.*;

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
    public abstract String evaluate(AbstractBuild<?, ?> ab, TaskListener tl, String string) throws MacroEvaluationException, IOException, InterruptedException;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(this.macroName);
    }
    
    public static File scriptsFolder() {
        return new File(Jenkins.getActiveInstance().getRootDir(), EMAIL_TEMPLATES_DIRECTORY);
    }
    
    protected abstract ConfigProvider getConfigProvider();
    
    @Override
    public boolean hasNestedContent() {
        return false;
    }
    
    protected InputStream getFileInputStream(String fileName, String extension)
            throws FileNotFoundException {
        
        InputStream inputStream;
        if(fileName.startsWith("managed:")) {
            String managedFileName = fileName.substring(8);
            try {
                inputStream = getManagedFile(managedFileName);
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
        if (fileExt.equals("")) {
            fileName += extension;
        }        
        
        inputStream = getClass().getClassLoader().getResourceAsStream(
                "hudson/plugins/emailext/templates/" + fileName);

        if (inputStream == null) {
            File templateFile = new File(scriptsFolder(), fileName);
            
            // the file may have an extension, but not the correct one
            if(!templateFile.exists()) {
                fileName += extension;
                templateFile = new File(scriptsFolder(), fileName);
            }            
            inputStream = new FileInputStream(templateFile);
        }

        return inputStream;
    }
    
    private InputStream getManagedFile(String fileName) {
        InputStream stream = null;
        Plugin plugin = Jenkins.getActiveInstance().getPlugin("config-file-provider");
        if (plugin != null) {
            Config config = null;
            ConfigProvider provider = getConfigProvider();
            for (Config c : provider.getAllConfigs()) {
                if (c.name.equalsIgnoreCase(fileName) && provider.isResponsibleFor(c.id)) {
                    config = c;
                    break;
                }
            }

            if (config != null) {
               stream = new ByteArrayInputStream(config.content.getBytes());
            }
        }
        return stream;
    }
    
    protected String generateMissingFile(String type, String fileName) {
        return type + " file [" + fileName + "] was not found in $JENKINS_HOME/" + EMAIL_TEMPLATES_DIRECTORY + ".";
    }
    
    protected String getCharset(AbstractBuild<?, ?> build) {
        return ExtendedEmailPublisher.descriptor().getCharset();
    }    
}
