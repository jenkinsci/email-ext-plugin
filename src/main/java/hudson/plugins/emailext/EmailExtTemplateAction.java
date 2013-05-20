/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.emailext;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TaskListener;
import hudson.plugins.emailext.plugins.content.ScriptContent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 *
 * @author acearl
 */
public class EmailExtTemplateAction implements Action {
    
    private AbstractProject<?,?> project;

    public EmailExtTemplateAction(AbstractProject<?,?> project) {
        this.project = project;        
    }
    
    public String getIconFileName() {
        return "/plugin/email-ext/images/template-debugger.png";
    }

    public String getDisplayName() {
        return Messages.EmailExtTemplateAction_DisplayName();
    }

    public String getUrlName() {
        return "templateTest";
    }   
    
    private String renderError(Exception ex) {
        return "";
    }
    
    @JavaScriptMethod
    public String renderTemplate(String templateFile, String buildId) {
        String result;
        
        try {
            ScriptContent content = new ScriptContent();
            content.template = templateFile;
            AbstractBuild<?,?> build = project.getBuild(buildId);
            result = content.evaluate(build, TaskListener.NULL, "SCRIPT");
        } catch (Exception ex) {
            result = renderError(ex);
        } 
        
        return result;
    }
    
    public AbstractProject<?, ?> getProject() {
        return project;
    }
}
