package hudson.plugins.emailext.plugins.trigger.XNthFailureTrigger

f = namespace("/lib/form")

f.entry(title: _("Number Of Failures"), field: "requiredFailureCount") {
    f.number()
}
