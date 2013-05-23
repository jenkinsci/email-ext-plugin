dt("\${BUILD_LOG_MULTILINE_REGEX}")
dd() {
  span(_("Displays build log segments that match the regular expression."))
  dl() {
    dt("regex")
    dd(_("Segments of the build log that match this regular expression " 
        +"are included.  See also " + a(href: "http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html", "java.util.regex.Pattern")
        +". No default. Required parameter"))
  
    dt("maxMatches")
    dd(_("The maximum number of matches to include. If 0, all matches will be included. Defaults to 0."))
  
    dt("showTruncatedLines")
    dd(_("If true, include [...truncated ### lines...] lines. Defaults to true."))
  
    dt("substText")
    dd(_("If non-null, insert this text into the email rather than the entire segment. Defaults to null."))
  
    dt("escapeHtml")
    dd(_("If true, escape HTML. Defaults to false."))
  
    dt("matchedSegmentHtmlStyle")
    dd(_("If non-null, output HTML. Matched lines will become <b style=\"your-style-value\">"
        +"html escaped matched lines</b>. Defaults to null."))
  }
}