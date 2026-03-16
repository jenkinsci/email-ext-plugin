dt("\${TEMPLATE}")
dd() {
  span(_("Custom message content generated from a template file. " +
     "The plugin will search for templates in the following order: " +
     "(1) managed files (prefix with <tt>managed:</tt>), " +
     "(2) workspace (relative path from workspace root), " +
     "(3) plugin resources, and " +
     "(4) <tt>\$JENKINS_HOME/email-templates</tt>. " +
     "To use managed files (Config File Provider plugin), use the \"Custom File\" config type and prefix the file NAME with \"managed:\". " + 
     "To use workspace templates, specify a relative path from workspace root. " +
     "(Example: <tt>\${TEMPLATE, file=\"managed:ManagedFileName\"}</tt> or <tt>\${TEMPLATE, file=\"templates/custom.txt\"}</tt>)"))
  
  dl() {
    dt("file")
    dd("The template in plain text format.")
  }
}
