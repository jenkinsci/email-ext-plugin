f = namespace("/lib/form")

f.section(title: _("Extended E-mail Notification")) {
  f.entry {
    p {
      text(_("Extended E-mail Notification settings have moved."))
      text(" ")
      a(href: "${rootURL}/manage/email-ext/", _("Configure Extended E-mail Notification"))
    }
  }
}
