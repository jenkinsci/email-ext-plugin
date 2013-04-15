dt("\${SCRIPT}")
dd() {
  span(_("Custom message content generated from a groovy script." +
     "Custom scripts should be placed in \"\$JENKINS_HOME/email-templates\". "+
     "When using a custom script the plugin will look in the resources for" +
     "the email-ext plugin first, and then in the \$JENKINS_HOME/email-templates" + 
     "directory. No other directories will be searched."))

  dl() {
    dt("script")
    dd("When this is used, only the last value in the script will be used in the expansion (script and template can not be used together).")
  
    dt("template")
    dd("The template in Groovy's SimpleTemplateEngine format.")
  }
}