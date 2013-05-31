f = namespace("/lib/form")

f.entry(title: _("Trigger Script"), help: "/plugin/email-ext/help/projectConfig/trigger/ScriptTrigger.html") {
  f.textarea(name: "triggerScript", value: instance?.triggerScript)
}