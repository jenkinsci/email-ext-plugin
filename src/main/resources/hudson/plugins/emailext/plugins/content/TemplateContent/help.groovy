dt("\${TEMPLATE}")
dd() {
  span(_("Custom message content generated from a template file. " +
     "Custom templates should be placed in <tt>\$JENKINS_HOME/email-templates</tt>. "+
     "When using a custom script the plugin will look in the resources for" +
     "the email-ext plugin first, and then in the <tt>\$JENKINS_HOME/email-templates</tt>" + 
     "directory. No other directories will be searched. " +
     "You may also use the Config File Provider plugin to manage your templates. " +
     "Use the \"Custom File\" config type for the template. " + 
     "Prefix the managed file NAME with \"managed:\" for the file " +
     "parameter. (Example: <tt>\${TEMPLATE, file=\"managed:ManagedFileName\"}</tt>)"))
  
  dl() {
    dt("file")
    dd("The template in plain text format.")
  }
}
