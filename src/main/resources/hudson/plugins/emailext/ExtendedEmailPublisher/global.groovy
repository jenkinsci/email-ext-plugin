// Namespaces
f = namespace("/lib/form")
d = namespace("jelly:define")
j = namespace("jelly:core")
l = namespace("/lib/layout")
st = namespace("jelly:stapler")
t = namespace("/lib/hudson")
m = namespace("/lib/email-ext")

f.section(title: _("Extended E-mail Notification")) {
  script(src: "${rootURL}/plugin/email-ext/scripts/emailext-behavior.js", type: "text/javascript") 
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/override-global-settings.html", checked: descriptor.overrideGlobalSettings, name: "ext_mailer_override_global_settings", title: _("Override Global Settings")) {
    f.entry(help: "/descriptor/hudson.tasks.Mailer/help/smtpServer", title: _("SMTP server")) {
      input(type: "text", class: "setting-input", value: descriptor.smtpServer, name: "ext_mailer_smtp_server") 
    }
    f.entry(help: "/descriptor/hudson.tasks.Mailer/help/defaultSuffix", title: _("Default user E-mail suffix")) {
      input(type: "text", class: "setting-input", value: descriptor.defaultSuffix, name: "ext_mailer_default_suffix") 
    }
    f.entry(help: "/descriptor/hudson.tasks.Mailer/help/adminAddress", title: _("System Admin E-mail Address")) {
      f.textbox(checkUrl: "'${rootURL}/publisher/Mailer/addressCheck?value='+encode(this.value)", name: "ext_mailer_admin_address", value: descriptor.adminAddress) 
    }
    f.entry(help: "/descriptor/hudson.tasks.Mailer/help/url", title: _("Jenkins URL")) {
      input(type: "text", class: "setting-input", value: h.ifThenElse(descriptor.hudsonUrl!=null,descriptor.hudsonUrl,h.inferHudsonURL(request)), name: "ext_mailer_hudson_url") 
    }
    f.advanced() {
      f.optionalBlock(help: "/help/tasks/mailer/smtpAuth.html", checked: descriptor.smtpAuthUsername!=null, name: "extmailer.useSMTPAuth", title: _("Use SMTP Authentication")) {
        f.entry(title: _("User Name")) {
          input(type: "text", class: "setting-input", value: descriptor.smtpAuthUsername, name: "extmailer.SMTPAuth.userName") 
        }
        f.entry(title: _("Password")) {
          input(type: "password", class: "setting-input", value: descriptor.smtpAuthPassword, name: "extmailer.SMTPAuth.password") 
        }
      }
      f.entry(help: "/descriptor/hudson.tasks.Mailer/help/useSsl", title: _("Use SSL")) {
        f.checkbox(checked: descriptor.useSsl, name: "ext_mailer_smtp_use_ssl") 
      }
      f.entry(help: "/descriptor/hudson.tasks.Mailer/help/smtpPort", title: _("SMTP port")) {
        input(type: "text", class: "setting-input", value: descriptor.smtpPort, name: "ext_mailer_smtp_port") 
      }
      f.entry(title: _("Charset")) {
        input(type: "text", class: "setting-input", value: descriptor.charset, name: "ext_mailer_charset") 
      }
    }
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/contentType.html", title: _("Default Content Type")) {
    select(class: "setting-input", name: "ext_mailer_default_content_type") {
      f.option(selected: 'text/plain'==descriptor.defaultContentType, value: "text/plain", _("contentType.plainText")) 
      f.option(selected: 'text/html'==descriptor.defaultContentType, value: "text/html", _("contentType.html")) 
    }
  }
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/listId.html", checked: descriptor.listId!=null, name: "extmailer.useListID", title: _("Use List-ID Email Header")) {
    f.entry(title: _("List ID")) {
      input(type: "text", class: "setting-input", value: descriptor.listId, name: "extmailer.ListID.id") 
    }
  }
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/precedenceBulk.html", checked: descriptor.precedenceBulk, name: "extmailer.addPrecedenceBulk", title: _("Add 'Precedence: bulk' Email Header")) 
  f.entry(field: "recipients", help: "/plugin/email-ext/help/globalConfig/defaultRecipients.html", title: _("Default Recipients")) {
    input(type: "text", class: "setting-input", value: descriptor.defaultRecipients, name: "ext_mailer_default_recipients") 
  }
  f.entry(field: "replyTo", help: "/plugin/email-ext/help/globalConfig/replyToList.html", title: _("Reply To List")) {
    input(type: "text", class: "setting-input", value: descriptor.defaultReplyTo, name: "ext_mailer_default_replyto") 
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/emergencyReroute.html", title: _("Emergency reroute")) {
    input(type: "text", class: "setting-input", value: descriptor.emergencyReroute, name: "ext_mailer_emergency_reroute") 
  }
  f.entry(help: "/plugin/email-ext/help/globalConfig/excludedCommitters.html", title: _("Excluded Committers")) {
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
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/debugMode.html", checked: descriptor.isDebugMode(), name: "ext_mailer_debug_mode", title: _("Enable Debug Mode")) 
  f.optionalBlock(help: "/plugin/email-ext/help/globalConfig/security.html", checked: descriptor.isSecurityEnabled(), name: "ext_mailer_security_enabled", title: _("Enable Security")) 
  tr() {
    td() 
    td(colspan: "2", _("Content Token Reference")) 
    td() {      
      a(href: "#contentTokenHelpAnchor", name: "contentTokenAnchor", onclick: "toggleContentTokenHelp('');return false") {
        img(src: "${rootURL}/images/16x16/help.gif", alt: _("Help for feature: Content Token Reference")) 
      }
    }
  }
  tr() {
    td() 
    td(colspan: "2") {      
      div(style: "display:none", id: "contentTokenHelpConf", class: "help") {
        m.tokenhelp(displayDefaultTokens: false)
      }
    }
    td() 
  }
}
