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
  }
}