package hudson.plugins.emailext;

import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.WithPlugin;

/**
 * Created by acearl on 9/15/2015.
 */
public class EmailExtStepTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    @WithPlugin("config-file-provider.hpi")
    public void configRoundTrip() throws Exception {
        EmailExtStep step1 = new EmailExtStep("subject", "body");
        step1.setTo("mickeymouse@disney.com");
        step1.setReplyTo("mickeymouse@disney.com");
        step1.setMimeType("text/html");

        EmailExtStep step2 = new StepConfigTester(r).configRoundTrip(step1);
        r.assertEqualDataBoundBeans(step1, step2);
    }
}
