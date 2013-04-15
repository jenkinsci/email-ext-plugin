dt("\${${my.MACRO_NAME}}")
dd {
  span(my.shortHelpDescription)
  dl() {
    dt("reverse")
    dd(_("If true, show most recent builds at the top instead of the bottom. Defaults to false."))
    
    dt("format")
    dd() {
      span(_("For each build listed, a string containing %X, where %X is one of"))
      dl() {
        dt("%c")
        dd(_("changes"))
        
        dt("%n")
        dd(_("build number"))
      }
      p(_("Defaults to ") + my.defaultFormatValue)
    }
  }
  span("See additional documentation on \${CHANGES} token for showPaths, format and pathFormat parameters")
}