/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.emailext;

import hudson.model.Hudson;
import jenkins.model.Jenkins;
import org.kohsuke.groovy.sandbox.GroovyValueFilter;

/**
 * Provides a sandbox for groovy scripts that disallows access to the Jenkins
 * instance.
 */
public class ScriptSandbox extends GroovyValueFilter {
    @Override
    public Object filter(Object o) {
        if (o instanceof Jenkins || o instanceof Hudson) {
            throw new SecurityException("Use of 'jenkins' is disallowed by security policy");
        }
        return o;
    }
}
