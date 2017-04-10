package hudson.plugins.emailext

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted

class ExtendedEmailPublisherTestHelper {
  @Whitelisted
  static def messageid() {
    "<12345@xxx.com>"
  }
}
