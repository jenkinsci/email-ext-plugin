dt("\${BUILD_LOG}")
dd() {
  span(_("Displays the end of the build log."))
  dl() {
    dt("maxLines")
    dd(_("Display at most this many lines of the log. Defaults to 250."))
  
    dt("escapeHtml")
    dd(_("If true, HTML is escape. Defaults to false."))
  }
}