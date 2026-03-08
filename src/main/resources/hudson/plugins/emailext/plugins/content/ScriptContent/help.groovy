dt("\${SCRIPT}")
dd() {
  span(_("Custom message content generated from a Groovy script. " +
     "The plugin will search for scripts in the following order: " +
     "(1) managed files (prefix with <tt>managed:</tt>), " +
     "(2) workspace (relative path from workspace root), " +
     "(3) plugin resources, and " +
     "(4) <tt>\$JENKINS_HOME/email-templates</tt>. " +
     "To use managed files (Config File Provider plugin), prefix the file NAME with <tt>managed:</tt>. " +
     "To use workspace templates, specify a relative path from workspace root. " +
     "(Example: <tt>\${SCRIPT, template=\"managed:ManagedFileName\"}</tt> or <tt>\${SCRIPT, template=\"email-templates/custom.groovy\"}</tt>)"))

  dl() {
    dt("script")
    dd("When this is used, only the last value in the script will be used in the expansion (script and template can not be used together).")

    dt("template")
    dd("The template in Groovy's SimpleTemplateEngine format.")
  }
}
