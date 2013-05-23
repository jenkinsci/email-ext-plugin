f = namespace("/lib/form")

f.entry(title: _("Trigger Script"), help: "/plugin/email-ext/help/projectConfig/trigger/ScriptTrigger.html") {
  f.textarea(clazz: "setting-input", name: "email_ext_prebuildscripttrigger_script", value: my.triggerScript)
}