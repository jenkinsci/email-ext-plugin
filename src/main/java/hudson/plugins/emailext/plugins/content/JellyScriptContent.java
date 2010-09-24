package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.XMLOutput;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JellyScriptContent
    implements EmailContent
{
    private static final Logger LOGGER = Logger.getLogger( JellyScriptContent.class.getName() );

    public static final String TEMPLATE_NAME_ARG = "template";

    private static final String DEFAULT_HTML_TEMPLATE_NAME = "html";

    private static final String DEFAULT_TEXT_TEMPLATE_NAME = "text";

    private static final String DEFAULT_TEMPLATE_NAME = DEFAULT_HTML_TEMPLATE_NAME;

    private static final String EMAIL_TEMPLATES_DIRECTORY = "email-templates";

    public String getToken()
    {
        return "JELLY_SCRIPT";
    }

    public String getHelpText()
    {
        return "Custom message content generated from a Jelly script template. " +
                "There are two templates provided: \"" + DEFAULT_HTML_TEMPLATE_NAME + "\" " +
                "and \"" + DEFAULT_TEXT_TEMPLATE_NAME + "\". Custom Jelly templates should be placed in " +
                "$HUDSON_HOME/" + EMAIL_TEMPLATES_DIRECTORY + ". When using custom templates, " +
                "the template filename without \".jelly\" should be used for " +
                "the \"" + TEMPLATE_NAME_ARG + "\" argument.\n" +
                "<ul>\n" +
                "<li><i>" + TEMPLATE_NAME_ARG + "</i> - the template name.<br>\n" +
                "Defaults to \"" + DEFAULT_TEMPLATE_NAME + "\".\n" +
                "</ul>\n";
    }

//    return "Displays an environment variable.\n" +
//    "<ul>\n" +
//
//    "<li><i>" + VAR_ARG_NAME + "</i> - the name of the environment " +
//            "variable to display.  If \"\", show all.<br>\n" +
//    "Defaults to \"" + VAR_DEFAULT_VALUE + "\".\n" +
//
//    "</ul>\n";


    public List<String> getArguments()
    {
        return Collections.singletonList( TEMPLATE_NAME_ARG );
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(
        AbstractBuild<P, B> build, ExtendedEmailPublisher publisher, EmailType type, Map<String, ?> args )
        throws IOException, InterruptedException
    {
        InputStream inputStream = null;
        try
        {
            String templateName = Args.get( args, TEMPLATE_NAME_ARG, DEFAULT_TEMPLATE_NAME );
            inputStream = getTemplateInputStream( templateName );
            return renderContent( build, inputStream );
        }
        catch ( JellyException e )
        {
            LOGGER.log( Level.SEVERE, null, e );
            return "JellyException: " + e.getMessage();
        }
        finally
        {
            IOUtils.closeQuietly( inputStream );
        }
    }

    /**
     * Try to get the template from the classpath first before trying the file system.
     *
     * @param templateName
     * @return
     * @throws java.io.FileNotFoundException
     */
    private InputStream getTemplateInputStream( String templateName )
        throws FileNotFoundException
    {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
            "hudson/plugins/emailext/templates/" + templateName + ".jelly" );

        if ( inputStream == null )
        {
            final File templatesFolder = new File( Hudson.getInstance().getRootDir(), EMAIL_TEMPLATES_DIRECTORY );
            final File templateFile = new File( templatesFolder, templateName + ".jelly" );
            inputStream = new FileInputStream( templateFile );
        }

        return inputStream;
    }

    private String renderContent( AbstractBuild<?, ?> build, InputStream inputStream )
        throws JellyException, IOException
    {
        JellyContext context = createContext( new JellyScriptContentBuildWrapper( build ), build );
        Script script = context.compileScript( new InputSource( inputStream ) );
        if ( script != null )
        {
            return convert( context, script );
        }
        return null;
    }

    private String convert( JellyContext context, Script script )
        throws JellyTagException, IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream( 16 * 1024 );
        XMLOutput xmlOutput = XMLOutput.createXMLOutput( output );
        script.run( context, xmlOutput );
        xmlOutput.flush();
        xmlOutput.close();
        output.close();
        return output.toString();
    }

    private JellyContext createContext( Object it, AbstractBuild<?, ?> build )
    {
        JellyContext context = new JellyContext();
        context.setVariable( "it", it );
        context.setVariable( "build", build );
        context.setVariable( "project", build.getParent() );
        context.setVariable( "rooturl", ExtendedEmailPublisher.DESCRIPTOR.getHudsonUrl() );
        return context;
    }

    public boolean hasNestedContent()
    {
        return false;
    }
}
