dt("\${JELLY_SCRIPT,template=\"TEMPLATE_NAME\"}")
dd(_("Custom message content generated from a Jelly script template. "
    +"There are two templates provided: \"html\" and \"text\". "
    +"Custom Jelly templates should be placed in <tt>\$JENKINS_HOME/email-templates</tt>. "
    +"When using custom templates, the template filename without \".jelly\" "
    +"should be used for the \"template\" argument. "
    +"You may also use the Config File Provider plugin to manage your templates. "
    +"Prefix the managed file NAME with \"managed:\" for the template "
    +"parameter. (Example: <tt>\${JELLY_SCRIPT, template=\"managed:ManagedFileName\"}</tt>)"))
