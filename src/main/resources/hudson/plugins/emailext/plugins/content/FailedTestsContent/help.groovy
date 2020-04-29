dt("\${FAILED_TESTS}")
dd() {
  span("Displays failing unit test information, if any tests failed.")
  dl() {
    dt("showStack")
    dd("Shows stack trace in failing test output. Defaults to true.")

    dt("showMessage")
    dd("Shows error message in failing test output. Defaults to true.")

    dt("maxTests")
    dd("Display at most this many tests. No limit is set by default.")

    dt("onlyRegressions")
    dd("Display only the failing tests that are different from previous builds. Defaults to false.")

    dt("escapeHtml")
    dd("If set to true escapes characters in errors details and stack traces using HTML entities. Defaults to false.")

    dt("testNameRegexPattern")
    dd("Display failed tests of the modules whose name match the regex given. Defaults to showing all failed tests")
  }
}