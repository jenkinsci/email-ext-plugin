// Namespaces
f = namespace("/lib/form")
d = namespace("jelly:define")
j = namespace("jelly:core")
l = namespace("/lib/layout")
st = namespace("jelly:stapler")
t = namespace("/lib/hudson")

f.section(title: _("Extended E-mail Notification")) {
  f.entry(field: "mailAccount") {
    f.property()
  }

  f.entry(field: "defaultSuffix", help: "/descriptor/hudson.tasks.Mailer/help/defaultSuffix", title: _("Default user e-mail suffix")) {
    f.textbox()
  }

  f.advanced() {
    f.entry(field: "charset", title: _("Charset")) {
      f.textbox()
    }

    f.entry(title: _("Additional accounts")) {
      f.repeatableProperty(field: "addAccounts", header: "Account") {
        div(align: "right") {
          f.repeatableDeleteButton()
        }
      }
    }
  }

  f.entry(field: "defaultContentType", help: "/plugin/email-ext/help/globalConfig/contentType.html", title: _("Default Content Type")) {
    f.select()
  }
  f.entry(field: "listId", help: "/plugin/email-ext/help/globalConfig/listId.html", title: _("List ID")) {
    f.textbox()
  }
  f.entry(field:"precedenceBulk", title: _("Add 'Precedence: bulk' E-mail Header"), help: "/plugin/email-ext/help/globalConfig/precedenceBulk.html") {
    f.checkbox()
  }
  f.entry(field: "defaultRecipients", help: "/plugin/email-ext/help/globalConfig/defaultRecipients.html", title: _("Default Recipients")) {
    f.textbox()
  }
  f.entry(field: "defaultReplyTo", help: "/plugin/email-ext/help/globalConfig/replyToList.html", title: _("Reply To List")) {
    f.textbox()
  }
  f.entry(field: "emergencyReroute", help: "/plugin/email-ext/help/globalConfig/emergencyReroute.html", title: _("Emergency reroute")) {
    f.textbox()
  }
  f.entry(field: "allowedDomains", help: "/plugin/email-ext/help/globalConfig/allowedDomains.html", title: _("Allowed Domains")) {
    f.textbox()
  }
  f.entry(field: "excludedCommitters", help: "/plugin/email-ext/help/globalConfig/excludedRecipients.html", title: _("Excluded Recipients")) {
    f.textbox()
  }
  f.entry(field: "defaultSubject", help: "/plugin/email-ext/help/globalConfig/defaultSubject.html", title: _("Default Subject")) {
    f.textbox()
  }
  f.entry(field: "maxAttachmentSizeMb", help: "/plugin/email-ext/help/globalConfig/maxAttachmentSize.html", title: _("Maximum Attachment Size")) {
    f.number(checkUrl: "'${rootURL}/publisher/ExtendedEmailPublisher/maxAttachmentSizeCheck?value='+encodeURIComponent(this.value)")
  }
  f.entry(field: "defaultBody", help: "/plugin/email-ext/help/globalConfig/defaultBody.html", title: _("Default Content")) {
    f.textarea()
  }
  f.entry(field: "defaultPresendScript", help: "/plugin/email-ext/help/globalConfig/defaultPresendScript.html", title: _("Default Pre-send Script")) {
    f.textarea()
  }
  f.entry(field: "defaultPostsendScript", help: "/plugin/email-ext/help/globalConfig/defaultPostsendScript.html", title: _("Default Post-send Script")) {
    f.textarea()
  }
  f.entry(title: _("Additional groovy classpath"), help: "/plugin/email-ext/help/globalConfig/defaultClasspath.html") {
    f.repeatableProperty(field: "defaultClasspath") {
      div(align: "right") {
        f.repeatableDeleteButton()
      }
    }
  }

  f.entry(field: "debugMode", title: _("Enable Debug Mode"), help: "/plugin/email-ext/help/globalConfig/debugMode.html") {
    f.checkbox()
  }
  f.entry(field: "adminRequiredForTemplateTesting",title: _("Require Administrator for Template Testing"), help: "/plugin/email-ext/help/globalConfig/requireAdmin.html") {
    f.checkbox()
  }
  f.entry(field: "watchingEnabled", title: _("Enable watching for jobs"), help: "/plugin/email-ext/help/globalConfig/watching.html") {
    f.checkbox()
  }
  f.entry(field: "allowUnregisteredEnabled", title: _("Allow sending to unregistered users"), help: "/plugin/email-ext/help/globalConfig/allowUnregistered.html") {
    f.checkbox()
  }

  f.advanced(title: _("Default Triggers")) {
    f.entry(title: _("Default Triggers"), help: "/plugin/email-ext/help/globalConfig/defaultTriggers.html") {
      hudson.plugins.emailext.plugins.EmailTrigger.all().each { t ->
        f.checkbox(name: "_.defaultTriggerIds", title: t.displayName, checked: descriptor.defaultTriggerIds.contains(t.id), json: t.id)
        br()
      }
    }
  }

  f.entry(title: _("Content Token Reference"), field:"tokens")
}
