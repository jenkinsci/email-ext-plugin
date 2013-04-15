dt("\${JELLY_SCRIPT,template=\"TEMPLATE_NAME\"}")
dd(_("Custom message content generated from a Jelly script template. "
    +"There are two templates provided: \"html\" and \"text\". "
    +"Custom Jelly templates should be placed in \$JENKINS_HOME/email-templates."
    +"When using custom templates, the template filename without \".jelly\" "
    +"should be used for the \"template\" argument."))