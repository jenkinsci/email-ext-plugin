// Namespaces
m = namespace("/lib/email-ext")
l = namespace("/lib/layout")
st = namespace("jelly:stapler")
j = namespace("jelly:core")
t = namespace("/lib/hudson")
f = namespace("/lib/form")
d = namespace("jelly:define")

def providers = hudson.plugins.emailext.plugins.RecipientProvider.all()
def recipientProviders = instance != null ? instance.email.recipientProviders : descriptor.defaultRecipientProviders
def contentType = instance?.email?.contentType ?: "project"

if (showSendTo) {
  f.entry(title: _("Send To")) {
    f.hetero_list(name: "recipientProviders", hasHeader: true, descriptors: providers, items: recipientProviders, oneEach: true)
  }
} else {
  f.invisibleEntry(name: "recipientProviders")
}

f.advanced {
  st.include(it: instance, class: descriptor.clazz, page: "local-config", optional: true)

  if (showSendTo) {
    f.entry(title: _("Recipient List"), field: "recipientList", help: "/plugin/email-ext/help/projectConfig/mailType/recipientList.html") {
      f.textbox()
    }
    f.entry(title: _("Reply-To List"), field: "replyTo", help: "/plugin/email-ext/help/projectConfig/mailType/replyToList.html") {
      f.textbox(default: "\$PROJECT_DEFAULT_REPLYTO")
    }
  } else {
    f.invisibleEntry(name: "recipientList")
    f.invisibleEntry(name: "replyTo")
  }

  f.entry(title: _("Content Type"), help: "/plugin/email-ext/help/projectConfig/contentType.html") {
    div(class: "jenkins-select") {
      select(name: "contentType", class: "jenkins-select__input setting-input") {
        f.option(selected: "project" == contentType, value: "project", _("Project Content Type"))
        f.option(selected: "text/plain" == contentType, value: "text/plain", _("projectContentType.plainText"))
        f.option(selected: "text/html" == contentType, value: "text/html", _("projectContentType.html"))
        f.option(selected: "both" == contentType, value: "both", _("projectContentType.both"))
      }
    }
  }

  f.entry(title: _("Subject"), field: "subject", help: "/plugin/email-ext/help/projectConfig/mailType/subject.html") {
    f.textbox(default: "\$PROJECT_DEFAULT_SUBJECT")
  }
  f.entry(title: _("Content"), field: "body", help: "/plugin/email-ext/help/projectConfig/mailType/body.html") {
    f.textarea(default: "\$PROJECT_DEFAULT_CONTENT")
  }
  f.entry(title: _("Attachments"), field: "attachmentsPattern", help: "/plugin/email-ext/help/projectConfig/attachments.html", description: _("description", "http://ant.apache.org/manual/Types/fileset.html")) {
    f.textbox()
  }
  f.entry(title: _("Inline Attachments"), field: "inlineAttachmentsPattern", help: "/plugin/email-ext/help/projectConfig/inlineAttachments.html", description: _("description", "http://ant.apache.org/manual/Types/fileset.html")) {
    f.textbox()
  }

  f.entry(title: _("Attach Build Log"), field: "attachBuildLogMode", help: "/plugin/email-ext/help/projectConfig/attachBuildLog.html") {
    f.enum(default: "NONE") {
      raw(my.description)
    }
  }
}
