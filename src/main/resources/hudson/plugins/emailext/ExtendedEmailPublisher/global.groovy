// Namespaces
f = namespace("/lib/form")
d = namespace("jelly:define")
j = namespace("jelly:core")
l = namespace("/lib/layout")
st = namespace("jelly:stapler")
t = namespace("/lib/hudson")

f.section(title: _("Extended E-mail Notification")) {
  f.entry(help: "/descriptor/hudson.tasks.Mailer/help/smtpServer", title: _("SMTP server")) {
    input(type: "text", class: "setting-input", value: descriptor.mailAccount.smtpHost, name: "ext_mailer_smtp_server")
  }
  f.entry(help: "/descriptor/hudson.tasks.Mailer/help/defaultSuffix", title: _("Default user E-mail suffix")) {
    input(type: "text", class: "setting-input", value: descriptor.defaultSuffix, name: "ext_mailer_default_suffix")
  }
  f.advanced() {
    f.optionalBlock(help: "/help/tasks/mailer/smtpAuth.html", checked: descriptor.mailAccount.smtpUsername!=null, name: "ext_mailer_use_smtp_auth", title: _("Use SMTP Authentication")) {
      f.entry(field: "smtpUsername", title: _("User Name")) {
        f.textbox(name: "ext_mailer_smtp_username")
      }
      f.entry(field: "smtpPassword", title: _("Password")) {
        f.password(name: "ext_mailer_smtp_password")
      }
    }
    f.entry(title: _("Advanced Email Properties")) {
      f.textarea(class: "setting-input", value: descriptor.mailAccount.advProperties, name: "ext_mailer_adv_properties")
    }
    f.entry(help: "/descriptor/hudson.tasks.Mailer/help/useSsl", title: _("Use SSL")) {
      f.checkbox(checked: descriptor.mailAccount.useSsl, name: "ext_mailer_smtp_use_ssl")
    }
    f.entry(help: "/descriptor/hudson.tasks.Mailer/help/smtpPort", title: _("SMTP port")) {
      input(type: "text", class: "setting-input", value: descriptor.mailAccount.smtpPort, name: "ext_mailer_smtp_port")
    }
    f.entry(title: _("Charset")) {
      input(type: "text", class: "setting-input", value: descriptor.charset, name: "ext_mailer_charset")
    }

      f.entry(title: _("Additional accounts")) {
          f.repeatable(field: "addAccounts", header: "Account") {
              table() {
                  f.entry(field: "address", title: _("E-mail address")) {
                      f.textbox()
                  }
                  f.entry(field: "smtpHost", title: _("SMTP Server")) {
                      f.textbox()
                  }
                  f.entry(field: "smtpPort", title: _("SMTP Port")) {
                      f.textbox()
                  }
                  f.optionalBlock(field: "auth", title: _("Use SMTP Authentication"), inline: true) {
                      f.entry(field: "smtpUsername", title: _("User Name")) {
                          f.textbox()
                      }
                      f.entry(field: "smtpPassword", title: _("Password")) {
                          f.password()
                      }
                  }
                  f.entry(field: "useSsl", title: _("Use SSL")) {
                      f.checkbox()
                  }
                  f.entry(field: "advProperties", title: _("Advanced Email Properties")) {
                      f.textarea()
                  }
              }
              div(align: "right") {
                  f.repeatableDeleteButton()
              }
          }
      }

  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/contentType.html", title: _("Default Content Type")) {
    select(class: "setting-input", name: "ext_mailer_default_content_type") {
      f.option(selected: 'text/plain'==descriptor.defaultContentType, value: "text/plain", _("contentType.plainText"))
      f.option(selected: 'text/html'==descriptor.defaultContentType, value: "text/html", _("contentType.html"))
    }
  }
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/listId.html", checked: descriptor.listId!=null, name: "ext_mailer_use_list_id", title: _("Use List-ID Email Header")) {
    f.entry(title: _("List ID")) {
      input(type: "text", class: "setting-input", value: descriptor.listId, name: "ext_mailer_list_id")
    }
  }
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/precedenceBulk.html", checked: descriptor.precedenceBulk, name: "ext_mailer_add_precedence_bulk", title: _("Add 'Precedence: bulk' Email Header"))
  f.entry(field: "recipients", help: "/plugin/email-ext/help/globalConfig/defaultRecipients.html", title: _("Default Recipients")) {
    input(type: "text", class: "setting-input", value: descriptor.defaultRecipients, name: "ext_mailer_default_recipients")
  }
  f.entry(field: "replyTo", help: "/plugin/email-ext/help/globalConfig/replyToList.html", title: _("Reply To List")) {
    input(type: "text", class: "setting-input", value: descriptor.defaultReplyTo, name: "ext_mailer_default_replyto")
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/emergencyReroute.html", title: _("Emergency reroute")) {
    input(type: "text", class: "setting-input", value: descriptor.emergencyReroute, name: "ext_mailer_emergency_reroute")
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/allowedDomains.html", title: _("Allowed Domains")) {
    input(type: "text", class: "setting-input", value: descriptor.allowedDomains, name: "ext_mailer_allowed_domains")
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/excludedRecipients.html", title: _("Excluded Recipients")) {
    input(type: "text", class: "setting-input", value: descriptor.excludedCommitters, name: "ext_mailer_excluded_committers")
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/defaultSubject.html", title: _("Default Subject")) {
    input(type: "text", class: "setting-input", value: descriptor.defaultSubject, name: "ext_mailer_default_subject")
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/maxAttachmentSize.html", title: _("Maximum Attachment Size")) {
      if(descriptor.maxAttachmentSize>0){
        input(checkUrl: "'${rootURL}/publisher/ExtendedEmailPublisher/maxAttachmentSizeCheck?value='+encodeURIComponent(this.value)", type: "text", class: "setting-input", value: descriptor.maxAttachmentSizeMb, name: "ext_mailer_max_attachment_size")
      } else{
        input(checkUrl: "'${rootURL}/publisher/ExtendedEmailPublisher/maxAttachmentSizeCheck?value='+encodeURIComponent(this.value)", type: "text", class: "setting-input", value: "", name: "ext_mailer_max_attachment_size")
      }
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/defaultBody.html", title: _("Default Content")) {
    f.textarea(class: "setting-input", value: descriptor.defaultBody, name: "ext_mailer_default_body")
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/defaultPresendScript.html", title: _("Default Pre-send Script")) {
    f.textarea(class: "setting-input", value: descriptor.defaultPresendScript, name: "ext_mailer_default_presend_script")
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/defaultPostsendScript.html", title: _("Default Post-send Script")) {
    f.textarea(class: "setting-input", value: descriptor.defaultPostsendScript, name: "ext_mailer_default_postsend_script")
  }
  f.entry(title: _("Additional groovy classpath"), help: "/plugin/email-ext/help/globalConfig/defaultClasspath.html") {
    f.repeatable(field: "defaultClasspath") {
      f.textbox(field: "path", name: "ext_mailer_default_classpath")
      div(align: "right") {
        f.repeatableDeleteButton()
      }
    }
  }
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/debugMode.html", checked: descriptor.isDebugMode(), name: "ext_mailer_debug_mode", title: _("Enable Debug Mode"))
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/requireAdmin.html", checked: descriptor.isAdminRequiredForTemplateTesting(), name: "ext_mailer_require_admin_for_template_testing", title: _("Require Administrator for Template Testing"))
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/watching.html", checked: descriptor.isWatchingEnabled(), name: "ext_mailer_watching_enabled", title: _("Enable watching for jobs"))
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/allowUnregistered.html", checked: descriptor.isAllowUnregisteredEnabled(), name: "ext_mailer_allow_unregistered_enabled", title: _("Allow sending to unregistered users"))

  f.advanced(title: _("Default Triggers")) {
    f.entry(title: _("Default Triggers"), help: "/plugin/email-ext/help/globalConfig/defaultTriggers.html") {
      hudson.plugins.emailext.plugins.EmailTrigger.all().each { t ->
        f.checkbox(name: "defaultTriggers", title: t.displayName, checked: descriptor.defaultTriggerIds.contains(t.id), json: t.id)
        br()
      }
    }
  }

  f.entry(title: _("Content Token Reference"), field:"tokens")
}
