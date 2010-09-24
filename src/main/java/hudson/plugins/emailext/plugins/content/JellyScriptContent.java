package hudson.plugins.emailext.plugins.content;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.plugins.emailext.EmailType;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.plugins.EmailContent;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.XMLOutput;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JellyScriptContent
    implements EmailContent
{
    private static final String TEMPLATE_NAME_ARG = "template";

    private static final String DEFAULT_HTML_TEMPLATE_NAME = "html";

    private static final String DEFAULT_TXT_TEMPLATE_NAME = "text";

    public String getToken()
    {
        return "JELLY_SCRIPT";
    }

    public String getHelpText()
    {
        return "Custom message content generated from a jelly template. " +
            "If no template name is provided then the default \"" + DEFAULT_HTML_TEMPLATE_NAME +
            "\" template is used. " + "Make sure that the message content type is set to HTML then. " +
            "The other template that is available is \"" + DEFAULT_TXT_TEMPLATE_NAME +
            "\" which should be used with Plain Text content type. " + "You can create your own message template " +
            "- the best way to do it is to ask Hudson administrators for the default template, modify it, " +
            "and send it back to the administrators who will place it in email templates folder in Hudson home directory.\n" +
            "<ul>\n" + "<li><i>" + TEMPLATE_NAME_ARG + "</i> - the template name. <br>\n" + "Defaults to \"" +
            DEFAULT_HTML_TEMPLATE_NAME + "\".\n" + "</ul>\n";
    }

    public List<String> getArguments()
    {
        return Collections.singletonList( TEMPLATE_NAME_ARG );
    }

    public <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContent(
        AbstractBuild<P, B> build, ExtendedEmailPublisher publisher, EmailType type, Map<String, ?> args )
        throws IOException, InterruptedException
    {
        String templateName = Args.get( args, TEMPLATE_NAME_ARG, DEFAULT_HTML_TEMPLATE_NAME );
        InputStream inputStream =
            getClass().getClassLoader().getResourceAsStream( "hudson/plugins/emailext/templates/" + templateName + ".jelly" );
        if (inputStream == null)
        {
            final File templatesFolder = new File(Hudson.getInstance().getRootDir(), "email-templates");
            final File templateFile = new File(templatesFolder, templateName + ".jelly");
            inputStream = new FileInputStream(templateFile);
        }


        // TODO: Close inputstream...
        return getContentFromJelly( build, inputStream );
    }

    public boolean hasNestedContent()
    {
        return false;
    }

    private <P extends AbstractProject<P, B>, B extends AbstractBuild<P, B>> String getContentFromJelly(
        AbstractBuild<P, B> build, InputStream inputStream )
    {
        try
        {
            return renderContent( new JellyScriptContentBuildWrapper( build ), build, inputStream );
        }
        catch ( Exception e )
        {
            // TODO: Better error handling
            e.printStackTrace();
            return e.getMessage();
        }
    }

    private String renderContent( Object it, AbstractBuild<?, ?> build, InputStream inputStream )
        throws JellyException, IOException
    {
        JellyContext context = createContext( it, build );
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
}
