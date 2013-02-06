// Namespaces
m = namespace("/hudson/plugins/emailext/tags")
l = namespace("/lib/layout")
st = namespace("jelly:stapler")
j = namespace("jelly:core")
t = namespace("/lib/hudson")
f = namespace("/lib/form")
d = namespace("jelly:define")


def secId = "emailext-${h.generateId()}-"
def nonConfigTriggers = hudson.plugins.emailext.ExtendedEmailPublisher.getTriggersForNonConfiguredInstance()
if(instance!=null) {
  nonConfigTriggers = instance.nonConfiguredTriggers
}

st.once() {
  script(src: "${rootURL}/plugin/email-ext/scripts/emailext-behavior.js", type: "text/javascript") 
  script(type: "text/javascript", "window.emailExtInit = new Array();") 
}
f.entry(title: _("Project Recipient List"), help: "/plugin/email-ext/help/projectConfig/globalRecipientList.html", description: _("Comma-separated list of email address that should receive notifications for this project.")) {
  if(instance?.configured) {
    input(name: "recipientlist_recipients", value: instance.recipientList, class: "setting-input validated", checkUrl: "'${rootURL}/publisher/ExtendedEmailPublisher/recipientListRecipientsCheck?value='+encodeURIComponent(this.value)", type: "text") 
  }
  else{
    input(name: "recipientlist_recipients", value: "\$DEFAULT_RECIPIENTS", class: "setting-input validated", checkUrl: "'${rootURL}/publisher/ExtendedEmailPublisher/recipientListRecipientsCheck?value='+encodeURIComponent(this.value)", type: "text") 
  }
}
f.entry(title: _("Project Reply-To List"), help: "/plugin/email-ext/help/projectConfig/replyToList.html", description: _("Command-separated list of email address that should be in the Reply-To header for this project.")) {
  if(instance?.configured) {
    input(name: "project_replyto", value: instance.replyTo, class: "setting-input validated", checkUrl: "'${rootURL}/publisher/ExtendedEmailPublisher/recipientListRecipientsCheck?value='+encodeURIComponent(this.value)", type: "text") 
  }
  else{
    input(name: "project_replyto", value: "\$DEFAULT_RECIPIENTS", class: "setting-input validated", checkUrl: "'${rootURL}/publisher/ExtendedEmailPublisher/recipientListRecipientsCheck?value='+encodeURIComponent(this.value)", type: "text") 
  }
}
f.entry(title: _("Content Type"), help: "/plugin/email-ext/help/projectConfig/contentType.html") {
  select(name: "project_content_type", class: "setting-input") {
    f.option(selected: 'default'==instance?.contentType, value: "default", _("Default Content Type")) 
    f.option(selected: 'text/plain'==instance?.contentType, value: "text/plain", _("projectContentType.plainText")) 
    f.option(selected: 'text/html'==instance?.contentType, value: "text/html", _("projectContentType.html")) 
  }
}
f.entry(title: _("Default Subject"), help: "/plugin/email-ext/help/projectConfig/defaultSubject.html") {
  if(instance?.configured) {
    input(name: "project_default_subject", value: instance.defaultSubject, class: "setting-input", type: "text") 
  }
  else{
    input(name: "project_default_subject", value: "\$DEFAULT_SUBJECT", class: "setting-input", type: "text") 
  }
}
f.entry(title: _("Default Content"), help: "/plugin/email-ext/help/projectConfig/defaultBody.html") {
  if(instance?.configured) {
    f.textarea(name: "project_default_content", value: instance.defaultContent, class: "setting-input") 
  }
  else{
    f.textarea(name: "project_default_content", value: "\$DEFAULT_CONTENT", class: "setting-input") 
  }
}
f.entry(title: _("Attachments"), help: "/plugin/email-ext/help/projectConfig/attachments.html", description: _("description('http://ant.apache.org/manual/Types/fileset.html')")) {
  if(instance?.configured) {
    input(name: "project_attachments", value: instance.attachmentsPattern, class: "setting-input", type: "text") 
  }
  else{
    input(name: "project_attachments", value: "", class: "setting-input", type: "text") 
  }
}
f.entry(title: _("Attach Build Log"), help: "/plugin/email-ext/help/projectConfig/attachBuildLog.html") {
  f.checkbox(name: "project_attach_buildlog", checked: instance?.attachBuildLog) 
}
def contentTokenText = hudson.plugins.emailext.EmailExtHelp.getContentTokenHelpText()
tr() {
  td() 
  td(colspan: "2", _("Content Token Reference")) 
  td() {
    a(name: "contentTokenAnchor", onclick: "toggleContentTokenHelp('${secId}');return false", href: "#contentTokenHelpAnchor") {
      img(alt: "Help for feature: Content Token Reference", src: "${rootURL}/images/16x16/help.gif") 
    }
  }
}
tr() {
  td() 
  td(colspan: "2") {
    div(id: "${secId}contentTokenHelpConf", style: "display:none", class: "help", "${contentTokenText}") {
      if(descriptor.isTokenMacroAvailable()) {
        br() 
        b("Token Macro Plugin Tokens") 
        br() 
        br() 
        help() 
      }
    }
  }
  td() 
}
if(descriptor.isMatrixProject(it)) {
  f.entry(field: "matrixTriggerMode", title: _("Trigger for matrix projects")) {
    f.enum(it.description) 
  }
}
f.advanced() {
  tr() {
    td(colspan: "4") {
      f.entry(title: _("Pre-send Script"), help: "/plugin/email-ext/help/projectConfig/presendScript.html") {
        f.textarea(id: "project_presend_script", name: "project_presend_script", value: instance?.presendScript, class: "setting-input") 
      }
    }
  }
  tr() {
    td() 
    td(colspan: "3", style: "margin-left:10px") {
      table(width: "100%", cell_padding: "0", cell_spacing: "0") {
        tbody(id: "${secId}configured-email-triggers") {
          tr() {
            td() 
            td() {
              div(style: "font-weight:bold", _("Trigger")) 
            }
            td() {
              div(style: "font-weight:bold", _("Send To Recipient List")) 
            }
            td() {
              div(style: "font-weight:bold", _("Send To Committers")) 
            }
            td() {
              div(style: "font-weight:bold", _("Send To Requester")) 
            }
            td() {
              div(style: "font-weight:bold", _("Include Culprits")) 
            }
            td() {
              div(style: "font-weight:bold", _("More Configuration")) 
            }
            td() {
              div(style: "font-weight:bold", _("Remove")) 
            }
            td() {
              a(class: "help-button", href: "#", helpURL: "${rootURL}/plugin/email-ext/help/projectConfig/advancedFeatures.html") {
                img(alt: _("Help for feature: Advanced Features"), src: "${imagesURL}/16x16/help.gif") 
              }
            }
          }
          tr(class: "help-area") {
            td() 
            td(colspan: "6") {
              div(class: "help", _("Loading...")) 
            }
            td() 
          }
          if(instance) {
            instance.configuredTriggers.each() { trigger -> 
              m.mailtype(secId: secId, trigger: trigger, title: trigger.descriptor.triggerName, sendToList: trigger.defaultSendToList, sendToDevs: trigger.defaultSendToDevs, sendToRequester: trigger.defaultSendToRequester, configured: true, mailTypeObj: trigger.email, includeCulps: /*trigger.defaultIncludeCulprits*/ false, mailType: trigger.descriptor.mailerId) 
            }
          }
          
          tr(id: "${secId}after-last-configured-row") {
            td() 
            td(colspan: "6") {
              span(style: "font-weight:bold", _("Add a Trigger:}")) 
              span(style: "font-weight:bold") 
              select(id: "${secId}non-configured-options", onchange: "selectTrigger(this,'${secId}')") {
                option(value: "select", "select") 
                nonConfigTriggers.each() { trigger -> 
                  def triggerId = trigger.descriptor.mailerId
                  option(id: "${secId}${triggerId}option", value: triggerId, "${triggerId}") 
                }
              }
            }
            td() {
              a(class: "help-button", href: "#", helpURL: "${rootURL}/plugin/email-ext/help/projectConfig/addATrigger.html") {
                img(alt: _("Help for feature: Add a Trigger"), src: "${imagesURL}/16x16/help.gif") 
              }
            }
            td() 
          }
          tr(class: "help-area") {
            td() 
            td(colspan: "6") {
              div(class: "help", _("Loading...")) 
            }
            td() 
          }
        }
      }
      table(style: "display:none") {
        tbody(id: "${secId}non-configured-email-triggers") {
          nonConfigTriggers.each() { trigger -> 
            m.mailtype(secId: secId, trigger: trigger, title: trigger.descriptor.triggerName, sendToList: trigger.defaultSendToList, sendToDevs: trigger.defaultSendToDevs, sendToRequester: trigger.defaultSendToRequester, configured: false, mailTypeObj: trigger.email, includeCulps: false/*trigger.defaultSendToDevs*/, mailType: trigger.descriptor.mailerId) 
          }
        }
      }
      select(id: "${secId}configured-options", style: "display:none") {
        if(instance) {
          instance.configuredTriggers.each() { trigger -> 
            def triggerId = trigger.descriptor.mailerId
            option(id: "${secId}${triggerId}option", value: triggerId, "${triggerId}") 
          }
        }
      }
    }
  }
}
if(!instance?.configured) {
  script(type: "text/javascript", "if (!window.emailExtInit['${secId}']) addTrigger('Failure','${secId}');\r\nwindow.emailExtInit['${secId}'] = 1;") 
}
