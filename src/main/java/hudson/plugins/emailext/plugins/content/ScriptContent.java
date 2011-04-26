package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import org.apache.commons.io.IOUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.ScriptContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.Writer;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptContent implements EmailContent {

    private static final Logger LOGGER = Logger.getLogger(ScriptContent.class.getName());

    public static final String SCRIPT_NAME_ARG = "script";
    
    public static final String SCRIPT_TEMPLATE_ARG = "template";
    
    public static final String SCRIPT_INIT_ARG = "init";

    private static final String DEFAULT_HTML_SCRIPT_NAME = "email-ext.groovy";

    private static final String DEFAULT_SCRIPT_NAME = DEFAULT_HTML_SCRIPT_NAME;
    
    private static final String DEFAULT_TEMPLATE_NAME = "groovy-html.template";
    
    private static final boolean DEFAULT_INIT_VALUE = true;
    
    private static final String EMAIL_TEMPLATES_DIRECTORY = "email-templates";

    private ScriptEngineManager scriptEngineManager = null;

    public ScriptContent() {
    	scriptEngineManager = new ScriptEngineManager();
    }

    public String getToken() {
        return "SCRIPT_CONTENT";
    }

    public String getHelpText() {
        StringBuilder helpText = new StringBuilder("Custom message content generated from a script using JSR 223. "
                + "Custom scripts should be placed in "
                + "$JENKINS_HOME/" + EMAIL_TEMPLATES_DIRECTORY + ". When using custom scripts, "
                + "the script filename WITH .py/.rb/etc	should be used for "
                + "the \"" + SCRIPT_NAME_ARG + "\" argument.\n"
                + "templates and other items may be loaded using the\n"
                + "host.readFile(String fileName) function\n"
                + "the function will look in the resources for\n"
                + "the email-ext plugin first, and then in the $JENKINS_HOME/" + EMAIL_TEMPLATES_DIRECTORY + "\n"
                + "directory. No other directories will be searched.\n"
                + "<ul>\n"
                + "<li><i>" + SCRIPT_NAME_ARG + "</i> - the script name.<br>\n"
                + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Defaults to \"" + DEFAULT_SCRIPT_NAME + "\".</li>\n"                
                + "<li><i>" + SCRIPT_TEMPLATE_ARG + "</i> - the template filename.<br>\n"
                + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Defaults to \"" + DEFAULT_TEMPLATE_NAME + "\"</li>\n"
                + "<li><i>" + SCRIPT_INIT_ARG + "</i> - true to run the language's init script.<br>\n"
                + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Defaults to " + DEFAULT_INIT_VALUE + "</li>\n"
				+ "<li>Available Script Engines\n"
				+ "<ul>\n");
	
		for (ScriptEngineFactory fact : scriptEngineManager.getEngineFactories()) {
			String extensions = join(fact.getExtensions(), ",");
			helpText.append("<li><i>" + fact.getLanguageName() + "</i> - " + fact.getLanguageVersion() + " ("+ extensions + ")</li>\n");
		}
		helpText.append("</ul></ul>\n");
		return helpText.toString();
    }

    public List<String> getArguments() {
    	List<String> args = new ArrayList<String>();
    	args.add(SCRIPT_NAME_ARG);
    	args.add(SCRIPT_TEMPLATE_ARG);
    	args.add(SCRIPT_INIT_ARG);
    	return args;
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(
            AbstractBuild<P, B> build, ExtendedEmailPublisher publisher, EmailType type, Map<String, ?> args)
            throws IOException, InterruptedException {
        InputStream inputStream = null;        
        String scriptName = Args.get(args, SCRIPT_NAME_ARG, DEFAULT_SCRIPT_NAME);
        String templateName = Args.get(args, SCRIPT_TEMPLATE_ARG, DEFAULT_TEMPLATE_NAME);
        boolean runInit = Args.get(args, SCRIPT_INIT_ARG, DEFAULT_INIT_VALUE);
        
        try {
            inputStream = getFileInputStream(scriptName);
            return renderContent(build, inputStream, scriptName, templateName, runInit);
        } catch (ScriptException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return "Exception: " + e.getMessage();
        } catch (FileNotFoundException e) {
            String missingScriptError = generateMissingFile(scriptName, templateName);
            LOGGER.log(Level.SEVERE, missingScriptError);
            return missingScriptError;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private String generateMissingFile(String script, String template) {
        return "Script [" + script + "] or template [" + template + "] was not found in $JENKINS_HOME/" + EMAIL_TEMPLATES_DIRECTORY + ".";
    }

    /**
     * Try to get the script from the classpath first before trying the file system.
     *
     * @param scriptName
     * @return
     * @throws java.io.FileNotFoundException
     */
    private InputStream getFileInputStream(String fileName)
            throws FileNotFoundException {				
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
                "hudson/plugins/emailext/templates/" + fileName);
        if (inputStream == null) {
            final File scriptsFolder = new File(Hudson.getInstance().getRootDir(), EMAIL_TEMPLATES_DIRECTORY);
            final File scriptFile = new File(scriptsFolder, fileName);
            inputStream = new FileInputStream(scriptFile);
        }
        return inputStream;
    }

    private String renderContent(AbstractBuild<?, ?> build, InputStream inputStream, 
    		String scriptName, String templateName, boolean runInit)
            throws ScriptException, IOException {
		String rendered = "";
		ScriptEngine engine = createEngine(scriptName, templateName, runInit, new ScriptContentBuildWrapper(build), build);
		if(engine != null) {
			try {
				Object res = engine.eval(new InputStreamReader(inputStream));
				if(res != null) {
					rendered = res.toString();
				}
			} finally {
				inputStream.close();
			}
		}
        return rendered;
    }
    
    public String readFile(String fileName)
		throws FileNotFoundException, IOException, UnsupportedEncodingException {
		String result = "";
		InputStream inputStream = getFileInputStream(fileName);
		if(inputStream != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[2048];
			try {
				Reader reader = new BufferedReader(
					new InputStreamReader(inputStream, "UTF-8"));
				int n;
				while((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
				result = writer.toString();
			} finally {
					inputStream.close();
			}			
		}		
		return result;
	}

    private ScriptEngine createEngine(String scriptName, String templateName, boolean runInit, Object it, AbstractBuild<?, ?> build) 
		throws FileNotFoundException, IOException {
		String extension = scriptName.substring(scriptName.lastIndexOf('.') + 1);
				
		ScriptEngine engine = scriptEngineManager.getEngineByExtension(extension);
		if(engine != null) {
			ScriptContext context = engine.getContext();
			context.setAttribute("it", it, ScriptContext.GLOBAL_SCOPE);
			context.setAttribute("build", build, ScriptContext.GLOBAL_SCOPE);
			context.setAttribute("project", build.getParent(), ScriptContext.GLOBAL_SCOPE);
			context.setAttribute("rooturl", ExtendedEmailPublisher.DESCRIPTOR.getHudsonUrl(), ScriptContext.GLOBAL_SCOPE);
			context.setAttribute("host", this, ScriptContext.GLOBAL_SCOPE);
			context.setAttribute("template", templateName, ScriptContext.GLOBAL_SCOPE);
			
			if(runInit) {
				InputStream initFile = null;
				try {
					initFile = getFileInputStream(extension + "/init." + extension);
					if(initFile != null) {
						engine.eval(new InputStreamReader(initFile));
					}
				} catch(ScriptException e) {
					LOGGER.log(Level.SEVERE, "ScriptException on init file: " + e.toString());
				} catch(Exception e) {
					LOGGER.log(Level.SEVERE, "Exception on init file: " + e.toString());
				} finally {
					if(initFile != null) {
						initFile.close();
					}
				}
			}
		}
        return engine;
    }

    public boolean hasNestedContent() {
        return false;
    }

    private String join(List<String> s, String delimiter) {
		if(s.isEmpty()) return "";
		Iterator<String> iter = s.iterator();
		StringBuilder builder = new StringBuilder(iter.next());
		while(iter.hasNext()) {
			builder.append(delimiter);
			builder.append(iter.next());
		}
		return builder.toString();
    }
}
