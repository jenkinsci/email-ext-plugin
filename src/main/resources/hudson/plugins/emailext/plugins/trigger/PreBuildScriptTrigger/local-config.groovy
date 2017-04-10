f = namespace("/lib/form")

f.entry(title: _("Trigger Script"), help: "/plugin/email-ext/help/projectConfig/trigger/ScriptTrigger.html") //Looks a bit awkward but the help text needs to be shown somehow.
f.property(field: "secureTriggerScript")