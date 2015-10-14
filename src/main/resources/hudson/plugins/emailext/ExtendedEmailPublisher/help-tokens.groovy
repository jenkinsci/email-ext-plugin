import hudson.plugins.emailext.plugins.ContentBuilder;

j = namespace("jelly:core")
tm = namespace("/lib/token-macro")
f = namespace("/lib/form")
st = namespace("jelly:stapler")

p(_("instructions"))  
p("Examples: \$TOKEN, \${TOKEN}, \${TOKEN, count=100}, \${ENV, var=\"PATH\"}")
h3(_("Project Tokens"))
dl() {
  dt("\${DEFAULT_SUBJECT}")
  dd(_("defaultSubject"))

  dt("\${DEFAULT_CONTENT}")
  dd(_("defaultContent"))

  dt("\${DEFAULT_PRESEND_SCRIPT}")
  dd(_("defaultPresend"))

  dt("\${DEFAULT_POSTSEND_SCRIPT}")
  dd(_("defaultPostsend"))

  dt("\${PROJECT_DEFAULT_SUBJECT}")
  dd(_("projectDefaultSubject"))

  dt("\${PROJECT_DEFAULT_CONTENT}")
  dd(_("projectDefaultContent"))
}
br()
h3(_("Extended Email Publisher Specific Tokens"))
ContentBuilder.privateMacros.each() { tm ->
    st.include(it: tm, page:"help", optional: "false")
}
br()
h3(_("Token Macro Plugin Tokens"))
tm.help()