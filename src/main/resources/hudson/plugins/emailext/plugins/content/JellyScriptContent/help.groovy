dt("\${JELLY_SCRIPT,template=\"TEMPLATE_NAME\"}")
dd(_("Custom message content generated from a Jelly script template. "
    +"There are two templates provided: \"html\" and \"text\". "
    +"The plugin will search for templates in the following order: "
    +"(1) managed files (prefix with <tt>managed:</tt>), "
    +"(2) workspace (relative path from workspace root), "
    +"(3) plugin resources, and "
    +"(4) <tt>\$JENKINS_HOME/email-templates</tt>. "
    +"When using custom templates, the template filename without \".jelly\" "
    +"should be used for the \"template\" argument. "
    +"To use managed files (Config File Provider plugin), prefix the file NAME with \"managed:\". "
    +"To use workspace templates, specify a relative path from workspace root. "
    +"(Example: <tt>\${JELLY_SCRIPT, template=\"managed:ManagedFileName\"}</tt> or <tt>\${JELLY_SCRIPT, template=\"templates/custom\"}</tt>)"))
