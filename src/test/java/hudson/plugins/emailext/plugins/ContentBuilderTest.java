package hudson.plugins.emailext.plugins;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.util.StreamTaskListener;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.WithPlugin;

import java.io.IOException;
import java.lang.reflect.Field;


import static org.mockito.Mockito.*;

public class ContentBuilderTest
    extends HudsonTestCase
{
    private ExtendedEmailPublisher publisher;
    private StreamTaskListener listener;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        
        listener = new StreamTaskListener(System.out);
        
        publisher = mock(ExtendedEmailPublisher.class);
        publisher.defaultContent = "For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!";
        publisher.defaultSubject = "How would you like your very own AWESOME-O 4000?";
        publisher.recipientList = "ashlux@gmail.com";

        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "defaultBody" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, "Give me $4000 and I'll mail you a check for $40,000!" );
        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "defaultSubject" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, "Nigerian needs your help!" );

        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "recipientList" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, "ashlux@gmail.com" );
    }

    public void testTransformText_shouldExpand_$PROJECT_DEFAULT_CONTENT()
        throws IOException, InterruptedException
    {
        assertEquals(publisher.defaultContent, new ContentBuilder().transformText( "$PROJECT_DEFAULT_CONTENT", publisher, null,
                                                         mock( AbstractBuild.class ), listener ));
        assertEquals(publisher.defaultContent, new ContentBuilder().transformText( "${PROJECT_DEFAULT_CONTENT}", publisher, null,
                                                         mock( AbstractBuild.class ), listener ));
    }

    public void testTransformText_shouldExpand_$PROJECT_DEFAULT_SUBJECT()
        throws IOException, InterruptedException
    {
        assertEquals(publisher.defaultSubject, new ContentBuilder().transformText( "$PROJECT_DEFAULT_SUBJECT", publisher, null,
                                                         mock( AbstractBuild.class ), listener ));
        assertEquals(publisher.defaultSubject, new ContentBuilder().transformText( "${PROJECT_DEFAULT_SUBJECT}", publisher, null,
                                                         mock( AbstractBuild.class ), listener ));
    }
    
    
    public void testTransformText_shouldExpand_$DEFAULT_CONTENT()
        throws IOException, InterruptedException
    {
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultBody(),
                        new ContentBuilder().transformText( "$DEFAULT_CONTENT", publisher, null,
                                                            mock( AbstractBuild.class ), listener ) );
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultBody(),
                        new ContentBuilder().transformText( "${DEFAULT_CONTENT}", publisher, null,
                                                            mock( AbstractBuild.class ), listener ) );
    }

    public void testTransformText_shouldExpand_$DEFAULT_SUBJECT()
        throws IOException, InterruptedException
    {
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultSubject(),
                        new ContentBuilder().transformText( "$DEFAULT_SUBJECT", publisher, null,
                                                            mock( AbstractBuild.class ), listener ) );
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultSubject(),
                        new ContentBuilder().transformText( "${DEFAULT_SUBJECT}", publisher, null,
                                                            mock( AbstractBuild.class ), listener ) );
    }
    
    public void testTransformText_shouldExpand_$DEFAULT_RECIPIENT_LIST()
        throws IOException, InterruptedException
    {
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultRecipients(),
                        new ContentBuilder().transformText( "$DEFAULT_RECIPIENTS", publisher, null,
                                                        mock( AbstractBuild.class ), listener ) );
        assertEquals( ExtendedEmailPublisher.DESCRIPTOR.getDefaultRecipients(),
                        new ContentBuilder().transformText( "${DEFAULT_RECIPIENTS}", publisher, null,
                                                        mock( AbstractBuild.class ), listener ) );
    }
    
    public void testTransformText_noNPEWithNullDefaultSubjectBody() throws NoSuchFieldException, IllegalAccessException
    {
        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "defaultBody" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, null );
        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField( "defaultSubject" );
        f.setAccessible( true );
        f.set( ExtendedEmailPublisher.DESCRIPTOR, null );
        assertEquals( "", new ContentBuilder().transformText( "$DEFAULT_SUBJECT", publisher, null, mock (AbstractBuild.class ), listener));
        assertEquals( "", new ContentBuilder().transformText( "$DEFAULT_CONTENT", publisher, null, mock (AbstractBuild.class ), listener));
    }
}
