// Namespaces
l = namespace("/lib/layout")
st = namespace("jelly:stapler")
j = namespace("jelly:core")
t = namespace("/lib/hudson")
f = namespace("/lib/form")
d = namespace("jelly:define")

def triggers = hudson.plugins.emailext.plugins.EmailTrigger.all()
def configuredTriggers = instance != null ? instance.configuredTriggers : descriptor.defaultTriggerInstances
def contentType = instance?.contentType ?: "default"

f.entry(
    title: _("Disable Extended Email Publisher"),
    field: "disabled",
    help: "/plugin/email-ext/help/projectConfig/disable.html",
    description: _("Allows the user to disable the publisher, while maintaining the settings")
) {
    f.checkbox()
}

f.entry(title: _("Project From"), field: "from") {
    f.textbox()
}
f.entry(
    title: _("Project Recipient List"),
    field: "recipientList",
    help: "/plugin/email-ext/help/projectConfig/globalRecipientList.html",
    description: _("Comma-separated list of email address that should receive notifications for this project.")
) {
    f.textarea(default: "\$DEFAULT_RECIPIENTS", checkUrl: "${rootURL}/publisher/ExtendedEmailPublisher/recipientListRecipientsCheck", checkDependsOn: "")
}
f.entry(
    title: _("Project Reply-To List"),
    field: "replyTo",
    help: "/plugin/email-ext/help/projectConfig/replyToList.html",
    description: _("Comma-separated list of email address that should be in the Reply-To header for this project.")
) {
    f.textarea(default: "\$DEFAULT_REPLYTO", checkUrl: "${rootURL}/publisher/ExtendedEmailPublisher/recipientListRecipientsCheck", checkDependsOn: "")
}
f.entry(title: _("Content Type"), help: "/plugin/email-ext/help/projectConfig/contentType.html") {
  div(class: "jenkins-select") {
    select(name: "contentType", class: "jenkins-select__input setting-input") {
      f.option(selected: "default" == contentType, value: "default", _("Default Content Type"))
      f.option(selected: "text/plain" == contentType, value: "text/plain", _("projectContentType.plainText"))
      f.option(selected: "text/html" == contentType, value: "text/html", _("projectContentType.html"))
      f.option(selected: "both" == contentType, value: "both", _("projectContentType.both"))
    }
  }
}
f.entry(title: _("Default Subject"), field: "defaultSubject", help: "/plugin/email-ext/help/projectConfig/defaultSubject.html") {
  f.textbox(default: "\$DEFAULT_SUBJECT")
}
f.entry(title: _("Default Content"), field: "defaultContent", help: "/plugin/email-ext/help/projectConfig/defaultBody.html") {
  f.textarea(default: "\$DEFAULT_CONTENT")
}
f.entry(title: _("Attachments"), field: "attachmentsPattern", help: "/plugin/email-ext/help/projectConfig/attachments.html", description: _("description", "http://ant.apache.org/manual/Types/fileset.html")) {
  f.textbox()
}
f.entry(title: _("Inline Attachments"), field: "inlineAttachmentsPattern", help: "/plugin/email-ext/help/projectConfig/inlineAttachments.html", description: _("description", "http://ant.apache.org/manual/Types/fileset.html")) {
  f.textbox()
}
f.entry(title: _("Attach Build Log"), field: "attachBuildLogMode", help: "/plugin/email-ext/help/projectConfig/attachBuildLog.html") {
  f.enum(default: descriptor.defaultAttachBuildLogMode) {
    raw(my.description)
  }
}

f.entry(title: _("Content Token Reference"), help: descriptor.getHelpFile("tokens"))

if (descriptor.isMatrixProject(my)) {
  f.entry(field: "matrixTriggerMode", title: _("Trigger for matrix projects")) {
    f.enum(default: "ONLY_PARENT") {
      raw(my.description)
    }
  }
}

f.advanced(title: _("Advanced Settings")) {
  f.entry(field: "presendScript", title: _("Pre-send Script"), help: "/plugin/email-ext/help/projectConfig/presendScript.html") {
    f.textarea(default: "\$DEFAULT_PRESEND_SCRIPT", class: "setting-input")
  }
  f.entry(field: "postsendScript", title: _("Post-send Script"), help: "/plugin/email-ext/help/projectConfig/postsendScript.html") {
    f.textarea(default: "\$DEFAULT_POSTSEND_SCRIPT", class: "setting-input")
  }
  f.entry(title: _("Additional groovy classpath"), help: "/plugin/help/projectConfig/defaultClasspath.html") {
    f.repeatable(field: "classpath") {
      f.textbox(field: "path")
      div(align: "right") {
        f.repeatableDeleteButton()
      }
    }
  }

  f.entry(title: _("Save to Workspace"), field: "saveOutput", help: "/plugin/email-ext/help/projectConfig/saveOutput.html") {
    f.checkbox()
  }

  showSendTo = true
  f.entry(title: _("Triggers"), help: "/plugin/email-ext/help/projectConfig/addATrigger.html") {
    f.hetero_list(
      name: "configuredTriggers",
      hasHeader: true,
      descriptors: triggers,
      items: configuredTriggers,
      addCaption: _("Add Trigger"),
      deleteCaption: _("Remove Trigger"),
      capture: "showSendTo"
    )
  }
}
