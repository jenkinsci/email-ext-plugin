// Namespaces
m = namespace("/lib/email-ext")
l = namespace("/lib/layout")
st = namespace("jelly:stapler")
j = namespace("jelly:core")
t = namespace("/lib/hudson")
f = namespace("/lib/form")
d = namespace("jelly:define")

f.entry(title: _("Send To"), help:"help") {
  br()
  f.checkbox(name: "sendToList", title: _("Recipients"), checked: instance != null ? instance.email.sendToRecipientList : descriptor.defaultSendToList)
  f.checkbox(name: "sendToDevs", title: _("Developers"), checked: instance != null ? instance.email.sendToDevelopers : descriptor.defaultSendToDevs)  
  f.checkbox(name: "sendToRequestor", title: _("Requestor"), checked: instance != null ? instance.email.sendToRequester : descriptor.defaultSendToRequester)
  f.checkbox(name: "sendToCulprits", title: _("Culprits"), checked: instance != null ? instance.email.sendToCulprits : descriptor.defaultSendToCulprits)
  f.advanced() {
    st.include(it: instance, class: descriptor.clazz, page: "local-config", optional: true)
    f.entry(title: _("Recipient List"), help: "/plugin/email-ext/help/projectConfig/mailType/recipientList.html") {
      f.textbox(name: "recipientList", value: instance != null ? instance.email.recipientList : "")
    }
    f.entry(title: _("Reply-To List"), help: "/plugin/email-ext/help/projectConfig/mailType/replyToList.html") {
      f.textbox(name: "replyTo", value: instance != null ? instance.email.replyTo : "\$PROJECT_DEFAULT_REPLYTO")
    }
    f.entry(title: _("Content Type"), help: "/plugin/email-ext/help/projectConfig/contentType.html") {
      select(name: "contentType", class: "setting-input") {
        f.option(selected: 'project'== (instance != null ? instance.email.contentType : ""), value: "project", _("Project Content Type")) 
        f.option(selected: 'text/plain'==(instance != null ? instance.email.contentType : ""), value: "text/plain", _("projectContentType.plainText")) 
        f.option(selected: 'text/html'==(instance != null ? instance.email.contentType : ""), value: "text/html", _("projectContentType.html")) 
      }
    }
    f.entry(title: _("Subject"), help: "/plugin/email-ext/help/projectConfig/mailType/subject.html") {
      f.textbox(name: "subject", value: instance != null ? instance.email.subject : "\$PROJECT_DEFAULT_SUBJECT")
    }
    f.entry(title: _("Content"), help: "/plugin/email-ext/help/projectConfig/mailType/body.html") {
      f.textarea(name: "body", value: instance != null ? instance.email.body : "\$PROJECT_DEFAULT_CONTENT")
    }
    f.entry(title: _("Attachments"), help: "/plugin/email-ext/help/projectConfig/attachments.html", description: _("description", "http://ant.apache.org/manual/Types/fileset.html")) {
       f.textbox(name: "attachmentsPattern", value: instance != null ? instance.email.attachmentsPattern : "")
    }
    f.entry(title: _("Attach Build Log"), help: "/plugin/email-ext/help/projectConfig/attachBuildLog.html") {
      select(name:"attachBuildLog") {
        f.option(value: 0, selected: instance != null ? !instance.email.attachBuildLog : true, _("Do Not Attach Build Log"))
        f.option(value: 1, selected: instance != null ? instance.email.attachBuildLog && !instance.email.compressBuildLog : false, _("Attach Build Log"))
        f.option(value: 2, selected: instance != null ? instance.email.attachBuildLog && instance.email.compressBuildLog : false, _("Compress and Attach Build Log"))
      }      
    }   
  }
}