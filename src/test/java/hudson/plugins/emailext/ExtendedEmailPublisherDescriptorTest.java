package hudson.plugins.emailext;

import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.StaplerRequest;
import static org.mockito.Mockito.*;

/**
 * Test the reading of settings and especially the hudsonUrl as it defines the rooturl for
 * jelly and groovy email scripts.
 */
public class ExtendedEmailPublisherDescriptorTest extends HudsonTestCase
{
    private String extMailerHudsonUrl;
    private String override;

    public void testNoOverrideUrl() throws Descriptor.FormException {
        ExtendedEmailPublisherDescriptor descriptor = new ExtendedEmailPublisherDescriptor();
        JSONObject json = new JSONObject();
        override = "false";
        extMailerHudsonUrl = "http://somethingelse.com/";

        StaplerRequest req = mockStaplerRequest();
        descriptor.configure(req, json);
        assertEquals(null, descriptor.getHudsonUrl());
    }

    public void testOverrideUrl() throws Descriptor.FormException {
        ExtendedEmailPublisherDescriptor descriptor = new ExtendedEmailPublisherDescriptor();
        JSONObject json = new JSONObject();
        override = "true";
        extMailerHudsonUrl = "http://somethingelse.com/";
        StaplerRequest req = mockStaplerRequest();
        descriptor.configure(req, json);
        assertEquals("http://somethingelse.com/", descriptor.getHudsonUrl());
    }

    private StaplerRequest mockStaplerRequest() {
        StaplerRequest req = mock(StaplerRequest.class);
        when(req.getParameter("ext_mailer_override_global_settings")).thenReturn(override);
        when(req.getParameter("ext_mailer_hudson_url")).thenReturn(extMailerHudsonUrl);
        when(req.getParameter("ext_mailer_smtp_server")).thenReturn(null);
        when(req.getParameter("ext_mailer_admin_address")).thenReturn(null);
        when(req.getParameter("ext_mailer_default_suffix")).thenReturn(null);
        when(req.getParameter("extmailer.useSMTPAuth")).thenReturn(null);
        when(req.getParameter("extmailer.SMTPAuth.userName")).thenReturn(null);
        when(req.getParameter("extmailer.SMTPAuth.password")).thenReturn(null);
        when(req.getParameter("ext_mailer_smtp_use_ssl")).thenReturn(null);
        when(req.getParameter("ext_mailer_smtp_port")).thenReturn(null);
        when(req.getParameter("ext_mailer_charset")).thenReturn(null);
        when(req.getParameter("ext_mailer_default_content_type")).thenReturn(null);
        when(req.getParameter("ext_mailer_default_subject")).thenReturn(null);
        when(req.getParameter("ext_mailer_default_body")).thenReturn(null);
        when(req.getParameter("ext_mailer_emergency_reroute")).thenReturn(null);
        when(req.getParameter("ext_mailer_max_attachment_size")).thenReturn(null);
        when(req.getParameter("ext_mailer_default_recipients")).thenReturn(null);
        when(req.getParameter("extmailer.addPrecedenceBulk")).thenReturn(null);
        when(req.getParameter("extmailer.useListID")).thenReturn(null);
        when(req.getParameter("extmailer.ListID.id")).thenReturn(null);
        return req;
    }
}
