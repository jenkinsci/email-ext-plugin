dt("\${CHANGES}")
dd() {
  span(_("Displays the changes since the last build."))
  dl() {
    dt("showDependencies")
    dd(_("If true, changes to projects this build depends on are shown. Defaults to false"))
  
    dt("showPaths")
    dd(_("If true, the paths, modifued by a commit are shown. Defaults to false"))
  
    dt("format")
    dd() {
      span(_("For each commit listed, a string containing %X, where %x is one of:"))
      dl() {
        dt("%a")
        dd(_("author"))
      
        dt("%d")
        dd(_("date"))
      
        dt("%m")
        dd(_("message"))
      
        dt("%p")
        dd(_("path"))
      
        dt("%r")
        dd(_("revision"))
      }
      p(_("Not all revision systems support %d and %r. If specified showPaths "
         +"argument is ignored. Defaults to \"[%a] %m\\\\n\""))
    }
  
    dt("pathFormat")
    dd(_("A string containing %p to indicate how to print paths. Defaults to "
        +"\"\\\\t%p\\\\n\""))
  }
}