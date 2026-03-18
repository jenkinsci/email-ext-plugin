# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- Added Configuration as Code (CasC) support for email templates, allowing templates to be provisioned via JCasC. (JENKINS-65558)
- Added logic to delete obsolete templates (`.groovy`, `.jelly`, `.template`) from `$JENKINS_HOME/email-templates/` before provisioning new templates via CasC to prevent stale files from accumulating.

### Fixed
- Fixed a security vulnerability by enforcing `Jenkins.ADMINISTER` permission instead of `Jenkins.MANAGE` for modifying global email template configurations via CasC.
- Fixed a cross-platform path validation bug (incompatible with Windows before Java 24) by replacing `File.getCanonicalPath()` with robust `java.nio.file.Path#startsWith` checks.
- Removed redundant security validations in `EmailTemplate.java` as they are already handled by the `SAFE_NAME_PATTERN` regex.
- Reverted unrelated code formatting changes introduced by auto-formatters to ensure a clean, reviewable diff for the PR.
