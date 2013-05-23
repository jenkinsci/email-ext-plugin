dt("\${BUILD_LOG_EXCERPT}")
dd() {
  span(_("Displays an excerpt from the build log."))
  dl() {
    dt("start")
    dd(_("Regular expression to match the excerpt starting line (matching line is excluded)."))
          
    dt("end")
    dd(_("Regular expression to match the excerpt ending line (matching line is excluded)."))
  }
  span() {
    raw(_("See <a href=\"http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html\"><i>java.util.regex.Pattern</i></a>"))
  }
}