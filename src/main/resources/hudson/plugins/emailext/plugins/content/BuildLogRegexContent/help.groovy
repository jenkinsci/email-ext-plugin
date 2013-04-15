dt("\${BUILD_LOG_REGEX}")
dd() {
  span(_("Displays lines from the build log that match the regular expression."))
  dl() {
    dt("regex")
    dd(_("Lines that match this regular expression are included. " +
         "See also java.util.regex.Pattern." + 
         "Defaults to \"(?i)\\\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\\\b\""))
    
    dt("linesBefore")
    dd(_("The number of lines to include before the matching line. " +
         "Lines that overlap with another match or linesAfter are only inlcuded once. " +
         "Defaults to 0."))
    
    dt("linesAfter")
    dd(_("The number of lines to include after the matching line. " +
         "Lines that overlap with another match or linesBefore are only included once. " +
         "Defaults to 0."))
  
    dt("maxMatches")
    dd(_("The maximum number of matches to include. If 0, all matches will be included. " +
         "Defaults to 0."))

    dt("showTruncatedLines")
    dd(_("If true, include [...truncated ### lines...] lines. " +
         "Defaults to true."))
    
    dt("substText")
    dd(_("If non-null, insert this text into the email rather than the " +
         "entire line. Defaults to null."))
   
    dt("escapeHtml")
    dd(_("If true, escape HTML. Defauts to false."))
  
    dt("matchedLineHtmlStyle")
    dd(_("If non-null, output HTML. Matched lines will become <b style=\"your-style-value\"> " +
         "html escaped matched line</b>. Defaults to null."))
    
    dt("addNewline")
    dd(_("If true, adds a newline after subsText. Defaults to true."))
  
    dt("defaultValue")
    dd(_("This value will be used if nothing is replaced."))
  }
}