dt("\${SCRIPT}")
dd() {
  span(_("Custom message content generated from a Groovy script. " +
     "Custom scripts should be placed in <tt>\$JENKINS_HOME/email-templates</tt>. "+
     "When using a custom script the plugin will look in the resources for" +
     "the email-ext plugin first, and then in the <tt>\$JENKINS_HOME/email-templates</tt>" +
     "directory. No other directories will be searched. " +
     "You may also use the Config File Provider plugin to manage your scripts. " +
     "Prefix the managed file NAME with <tt>managed:</tt> for the template or script " +
     "parameter. (Example: <tt>\${SCRIPT, template=\"managed:ManagedFileName\"}</tt>)"))

  dl() {
    dt("script")
    dd("When this is used, only the last value in the script will be used in the expansion (script and template can not be used together).")

    dt("template")
    dd("The template in Groovy's SimpleTemplateEngine format.")
  }
}
