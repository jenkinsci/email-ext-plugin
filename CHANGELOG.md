# Changelog

## 2.66 (March 21, 2019)

-   Fix some usages of "email" to "e-mail" to be consistent (thanks
    [VirtualTim](https://github.com/VirtualTim))
-   Update plugin to build and test with JDK11
    (thanks [batmat](https://github.com/batmat))

## 2.65 (March 6, 2019)

-   [Fix](https://jenkins.io/security/advisory/2019-03-06/#SECURITY-1340)
    [security
    issue](https://jenkins.io/security/advisory/2019-03-06/#SECURITY-1340)

## 2.63 (August 5, 2018)

-   allow filtering email domains we send emails to ([pull
    167](https://github.com/jenkinsci/email-ext-plugin/pull/167))
-   Help markup ([pull
    169](https://github.com/jenkinsci/email-ext-plugin/pull/169)) 
-   Same recipient in CC or BCC removes it from TO  [
    JENKINS-52748](https://issues.jenkins-ci.org/browse/JENKINS-52748) -
    Getting issue details... STATUS

## 2.62 (March 23, 2018)

-   Styling changes + change in test results table + code alignment
    ([pull 162](https://github.com/jenkinsci/email-ext-plugin/pull/162))

-   Fix link for RFC-2919 in help ([pull
    164](https://github.com/jenkinsci/email-ext-plugin/pull/164))

-   Update some libraries for some feature additions. ([pull
    165](https://github.com/jenkinsci/email-ext-plugin/pull/165))

-   The email-ext-plugin Pipeline integration is not very ergonomic  [
    JENKINS-49733](https://issues.jenkins-ci.org/browse/JENKINS-49733) -
    Getting issue details... STATUS

-   Check for attachment size does not consider compression  [
    JENKINS-49913](https://issues.jenkins-ci.org/browse/JENKINS-49913) -
    Getting issue details... STATUS

-   Add additional accounts ([pull
    166](https://github.com/jenkinsci/email-ext-plugin/pull/166))

## 2.61 (October 27, 2017)

-   Add global checkbox to allow sending emails to unregistered users
    ([pull 161](https://github.com/jenkinsci/email-ext-plugin/pull/161))
-   Switch to using RunWithSCM for getCulprits logic  [
    JENKINS-24141](https://issues.jenkins-ci.org/browse/JENKINS-24141) -
    Getting issue details... STATUS

## 2.60 (September 19, 2017)

-   groovy-text.template: use of member changeSet instead of changeSets
    ([JENKINS-38968](https://issues.jenkins-ci.org/browse/JENKINS-38968))

## 2.59 (September 12, 2017)

-   NullPointerException when calling addRecipients
    ([JENKINS-45529](https://issues.jenkins-ci.org/browse/JENKINS-45529))
-   groovy-html.template: use of member changeSet instead of changeSets
    ([JENKINS-38968](https://issues.jenkins-ci.org/browse/JENKINS-38968))

## 2.58 (Jun 29, 2017)

-   Make message clearer added in SECURITY-372
-   Add presend and postsend script support in pipeline
    ([\#157](https://github.com/jenkinsci/email-ext-plugin/pull/157 "Add presend and postsend script support in pipeline"))
-   allRecipients could be null if presend script remove all of them
    ([\#156](https://github.com/jenkinsci/email-ext-plugin/pull/156))

## 2.57.2 (April 10, 2017)

-   SECURITY-257([advisory](https://jenkins.io/security/advisory/2017-04-10/))
     Run Groovy and Jelly scripts only if approved or in secure sandbox

Jenkins administrators may need to approve scripts used by this plugin.
Administrators can either proactively review all job configurations for
Groovy scripts or they can wait for the jobs to run and fail. Approval
is performed via the [Script Security Plugin](https://plugins.jenkins.io/script-security/).

  

## 2.57.1 (March 20, 2017)

-   SECURITY-372
    ([advisory](https://jenkins.io/security/advisory/2017-03-20/))
    Emails were sent to addresses not associated with actual users of
    Jenkins.

If the security fix is undesirable in a particular instance, it can be
disabled with either or both of the following two system properties:

-   `-Dhudson.tasks.MailSender.SEND_TO_UNKNOWN_USERS=true`: send mail to
    build culprits even if they do not seem to be associated with a
    valid Jenkins login.
-   `-Dhudson.tasks.MailSender.SEND_TO_USERS_WITHOUT_READ=true`: send
    mail to build culprits associated with a valid Jenkins login even if
    they would not otherwise have read access to the job.

##  2.57 (February 18, 2017)

-   Allow using 'emailext' step in pipeline without 'node'/workspace
    context
    ([JENKINS-42140](https://issues.jenkins-ci.org/browse/JENKINS-42140))

## 2.56 (February 14, 2017)

-   Take 2 on previous

## 2.55 (February 11, 2017)

-   Bring back functions removed in JENKINS-40964 that are required for
    upgrade of existing config

## 2.54 (January 22, 2017)

-   update to latest config-file-provider
    ([JENKINS-40964](https://issues.jenkins-ci.org/browse/JENKINS-40964))

## 2.53 (December 23, 2016)

-   CulpritsRecipientProvider does not work with pipeline
    ([JENKINS-40653](https://issues.jenkins-ci.org/browse/JENKINS-40653))

## 2.52 (October 23, 2016)

-   enable setter for smtpHost in descriptor (PR-142)
-   Fix getter value in UI
    ([JENKINS-37995](https://issues.jenkins-ci.org/browse/JENKINS-37995))
-   Add API to programmatically configure global settings
    ([JENKINS-39147](https://issues.jenkins-ci.org/browse/JENKINS-39147))

## 2.51 (September 28, 2016)

-   Pipeline Support - Failed
    Tests ([JENKINS-38519](https://issues.jenkins-ci.org/browse/JENKINS-38519))

## 2.50 (September 24, 2016)

-   Template support in pipelines
    ([JENKINS-35367](https://issues.jenkins-ci.org/browse/JENKINS-35367))
-   X failure trigger
    ([JENKINS-37995](https://issues.jenkins-ci.org/browse/JENKINS-37995))
-   Don't use random for filename in Save to Workspace option
    ([JENKINS-37350](https://issues.jenkins-ci.org/browse/JENKINS-37350))

## 2.48 & 2.49 Failed releases

## 2.47 (August 7, 2016)

-   2nd failure emails being sent even when build is successful with
    job-dsl-plugin
    ([JENKINS-37188](https://issues.jenkins-ci.org/browse/JENKINS-37188))

## 2.46 (August 4, 2016)

-   emailext Pipeline step sends mails to irrelevant people
    ([JENKINS-37163](https://issues.jenkins-ci.org/browse/JENKINS-37163))

## 2.45 (July 31, 2016)

-   Non breaking spaces being double escaped - Thanks [Pierre-Gildas
    MILLON](https://issues.jenkins-ci.org/secure/ViewProfile.jspa?name=pgmillon)
    ([JENKINS-35669](https://issues.jenkins-ci.org/browse/JENKINS-35669))
-   NPE in email-ext FailingTestSuspectsRecipientProvider
    ([JENKINS-36402](https://issues.jenkins-ci.org/browse/JENKINS-36402))
-   Fix of findbugs reported issues
-   Upgrade to plugin pom 2.7

## 2.44 (June 13, 2016)

-   Extended Pipeline support
    ([JENKINS-35365](https://issues.jenkins-ci.org/browse/JENKINS-35365))

## 2.43 (June 4, 2016)

-   Fixed Content Token Reference throwing error if Config File Provider
    plugin is not installed ([issue
    35289](https://issues.jenkins-ci.org/browse/JENKINS-35289))
-   Fixed NPE when watching a job ([Issue
    33717](https://issues.jenkins-ci.org/browse/JENKINS-33717))
-   Merged pull request
    [130](https://github.com/jenkinsci/email-ext-plugin/pull/130)
-   Merged pull request
    [133](https://github.com/jenkinsci/email-ext-plugin/pull/133) Fix
    for [issue
    34785](https://issues.jenkins-ci.org/browse/JENKINS-34785)
-   Added ability to use a template from the workspace.

## 2.42 (April 17, 2016)

-   Fixed issue with post-send script not saving ([issue
    33205](https://issues.jenkins-ci.org/browse/JENKINS-33205) thanks
    to [weisslj](https://github.com/weisslj))
-   Fixed issue with non-English characters in file names for
    attachments ([issue
    33574](https://issues.jenkins-ci.org/browse/JENKINS-33574))
-   Fixed NPE issue when using groovy script ([issue
    33690](https://issues.jenkins-ci.org/browse/JENKINS-33690))
-   Added a license file
-   Moved to new parent pom version
-   Fixed up some tests

## 2.41.3 (Feb 23, 2016)

-   Fixed issue when workflow is not installed ([issue
    33035](https://issues.jenkins-ci.org/browse/JENKINS-33035))

## 2.41.2 (Feb 18, 2016)

-   Fixed issue with wrong class loader for templates
    ([issue 32910](https://issues.jenkins-ci.org/browse/JENKINS-32910))
-   Allow semicolon for address separator
    ([issue 32889](https://issues.jenkins-ci.org/browse/JENKINS-32889))

## 2.41 (Feb 07, 2016)

-   Cleaned up dependencies
-   Removed several tokens that were moved to token-macro
-   Fixed several items flagged by PMD (thanks Mohammed Ezzat**)**
-   Added post-send script feature similar to pre-send script.
    (Thanks [weisslj](https://github.com/weisslj))
-   Fixed issue with non-AbstractProject/Build items ([issue
    29970](https://issues.jenkins-ci.org/browse/JENKINS-29970)<https://issues.jenkins-ci.org/browse/JENKINS-29970>)
-   Fixed watching so it doesn't show recipient fields (issue 29449)

## 2.40.5 (Jun 08, 2015)

-   Whitespace and import cleanup
-   DefaultTriggers refactoring

## 2.40.4 (May 24, 2015)

-   Fix issue where the wrong config provider would be cached if using
    multiple types of managed templates.

## 2.40.3 (May 20, 2015)

-   Fix issue with incorrect handling of cc and bcc recipients ([issue
    \#28444](https://issues.jenkins-ci.org/browse/JENKINS-28444))
-   Fix issue with readResolve including Mailer components ([issue
    \#28402](https://issues.jenkins-ci.org/browse/JENKINS-28402))
-   Fix issue where default extension was not added to template name if
    there was something that looked like an extension in the name
    ([issue
    \#28357](https://issues.jenkins-ci.org/browse/JENKINS-28357))

## 2.40.2 (May 13, 2015)

-   Set the debug mode for JavaMail correctly when debug mode is enabled
    in global configuration
-   Fixed issue where the deserialization was not working correctly for
    descriptors ([issue
    \#28212](https://issues.jenkins-ci.org/browse/JENKINS-28212))

## 2.40.1 (May 4, 2015)

-   Fixed issue with classpath entries that have environment variables
    ([issue
    \#28145](https://issues.jenkins-ci.org/browse/JENKINS-28145))
-   Fixed issue with check of the extension for templates on the file
    system ([issue
    \#28202](https://issues.jenkins-ci.org/browse/JENKINS-28202))

## 2.40 (April 28, 2015)

-   Thanks to [K.R. Walker](https://github.com/krwalker), [Cédric
    Levasseur](https://github.com/CedricLevasseur)
-   Fixed issue with pre-send scripts not using 'cancel' correctly
    ([issue
    \#27448](https://issues.jenkins-ci.org/browse/JENKINS-27448))
-   Added ability to send email in HTML and plaintext with plaintext
    being a stripped version of the HTML ([issue
    \#23126](https://issues.jenkins-ci.org/browse/JENKINS-23126))
-   Added ability to configure the set of triggers that is setup by
    default when adding email-ext to a project ([issue
    \#27856](https://issues.jenkins-ci.org/browse/JENKINS-27856))
-   Moved away from using the Mailer plugin to create a session.
-   Added new TEMPLATE token that can be used to pull normal content
    from a file ([issue
    \#26478](https://issues.jenkins-ci.org/browse/JENKINS-26478))
-   Allow use of content tokens in the pre-send script ([issue
    \#26286](https://issues.jenkins-ci.org/browse/JENKINS-26286))
-   Allow sending console logs for all nodes in matrix build ([issue
    \#21861](https://issues.jenkins-ci.org/browse/JENKINS-21861))
-   Added ability for users to watch jobs ([issue
    \#18567](https://issues.jenkins-ci.org/browse/JENKINS-18567))
-   Removed admin email address since it should be set in the Jenkins
    Location area ([issue
    \#25926](https://issues.jenkins-ci.org/browse/JENKINS-25926))
-   Fixed output from CSSInliner that was escaping entities ([issue
    \#25719](https://issues.jenkins-ci.org/browse/JENKINS-25719))
-   Added FirstFailingBuildSuspectsRecipientProvider

## 2.39.2 (January 30, 2015)

-   Thanks to [Everspace](https://github.com/Everspace)
-   Add workaround for
    [JENKINS-25940](https://issues.jenkins-ci.org/browse/JENKINS-25940)
-   Added new SCRIPT\_CONTENT macro that can be used with pre-send
    scripts.
-   Added uberClassLoader to JellyContext

## 2.39 (November 16, 2014)

-   Thanks to [Christian
    Galsterer](https://github.com/christiangalsterer), [Alex
    Ouzounis](https://github.com/alexouzounis), [Gregory
    SSI-YAN-KAI](https://github.com/gssiyankai), [Jesse
    Glick](https://github.com/jglick), [Jeff
    Maury](https://github.com/jeffmaury),
    [robin-knight](https://github.com/robin-knight), [K.R.
    Walker](https://github.com/krwalker)
-   Updated to 1.554.1 as parent pom version
-   Added a recipient provider for upstream committers ([issue
    \#17742](https://issues.jenkins-ci.org/browse/JENKINS-17742))
-   Fixed issue with template testing not supporting managed files
    ([issue
    \#23619](https://issues.jenkins-ci.org/browse/JENKINS-23619))
-   Fixed typo in help file for CHANGES\_SINCE\_LAST\_BUILD
-   Added support of regex to replace/change the messages in CHANGES
    token ([issue
    \#23691](https://issues.jenkins-ci.org/browse/JENKINS-23691))
-   Fixed issue that attached build log is not complete ([issue
    \#23660](https://issues.jenkins-ci.org/browse/JENKINS-23660))
-   Added disable at the project level ([issue
    \#22154](https://issues.jenkins-ci.org/browse/JENKINS-22154))
-   Added caching of the templates to improve performance
-   Added parameter to CHANGES tokens to allow user to show a specific
    message when there are no changes ([issue
    \#20324](https://issues.jenkins-ci.org/browse/JENKINS-20324))
-   Added classpath support for the pre-send script ([issue
    \#21672](https://issues.jenkins-ci.org/browse/JENKINS-21672))
-   Added SMTP timeout so that jobs won't hang indefinitely 
-   Added retry for ConnectionExceptions ([issue
    \#16181](https://issues.jenkins-ci.org/browse/JENKINS-16181))
-   Added console output for template testing ([issue
    \#24063](https://issues.jenkins-ci.org/browse/JENKINS-24063))
-   Added check for test failure age in regression trigger ([issue
    \#22041](https://issues.jenkins-ci.org/browse/JENKINS-22041))
-   Added FailingTestSuspectsRecipientProvider

## 2.38.2 (August 26, 2014)

-   Switch to using getAction instead of getTestResultAction to work
    with newer versions of core.

## 2.38.1 (June 2, 2014)

-   Fix for NPE when no recipient providers are selected in a trigger.

## 2.38 (May 24, 2014)

-   Implemented new extension point for recipient providers
    (RecipientProvider) this changes the way that recipient types are
    added 
    -   There are no longer checkboxes for "Requestor" "Recipients" etc,
        each is provided by an implementation of a RecipientProvider
-   Fixed log zipping to remove annotations ([issue
    \#21180](https://issues.jenkins-ci.org/browse/JENKINS-21180))
-   Added help information for TRIGGER\_NAME token ([issue
    \#21912](https://issues.jenkins-ci.org/browse/JENKINS-21912))
-   Added support for managed files using the Config File Provider
    plugin, prefix managed file name with "managed:"
-   Removed dependency on the Maven plugin
-   Added more help and updated help messages to be more clear ([issue
    \#20384](https://issues.jenkins-ci.org/browse/JENKINS-20384))
-   Added bcc support ([issue
    \#21730](https://issues.jenkins-ci.org/browse/JENKINS-21730))
-   Added showMessage parameter to FAILED\_TESTS to allow turning off
    error messages
-   Cleaned up template to remove duplicate sections ([issue
    \#22592](https://issues.jenkins-ci.org/browse/JENKINS-22592))
-   Added %a and %d for pathFormat parameter of the CHANGES\_SINCE\*
    tokens ([issue
    \#20692](https://issues.jenkins-ci.org/browse/JENKINS-20692))

## 2.37.2.2 (March 8, 2014)

-   Added caching to the private macros to reduce time ([issue
    \#20078](https://issues.jenkins-ci.org/browse/JENKINS-20078))

## 2.37.2 (January 26, 2014)

-   Marked the Config subclass Extensions as optional, so the plugin
    doesn't cause issues if the Config File Provider plugin is not
    installed. ([issue
    \#21326](https://issues.jenkins-ci.org/browse/JENKINS-21326))

## 2.37.1 (January 11, 2014)

-   Fix issue with missing dependency on maven-plugin in  pom.xml

## 2.37 (January 8, 2014)

-   Updated parent pom version to 1.532.1 LTS
-   Allow use of managed scripts for Jelly or Groovy content. Use prefix
    "managed:" before name of managed script ([issue
    \#18203](https://issues.jenkins-ci.org/browse/JENKINS-18203))
-   Added new "Status Changed" trigger (thanks francois\_ritaly)
-   Added fileNotFoundMessage to FILE token ([issue
    \#20325](https://issues.jenkins-ci.org/browse/JENKINS-20325))
-   Added inline help for triggers that was missing ([issue
    \#20170](https://issues.jenkins-ci.org/browse/JENKINS-20170))
-   Changed BuildStepMonitor.NONE to allow concurrent builds (thanks
    jglick) ([issue
    \#16376](https://issues.jenkins-ci.org/browse/JENKINS-16376))\*\*
    You must take care when using content or triggers that look at
    previous builds if you enable concurrent builds for your project
-   Added TRIGGER\_NAME token so users can determine what trigger caused
    the email ([issue
    \#20265](https://issues.jenkins-ci.org/browse/JENKINS-20265))
-   Changed to Mutlimap so that you can add multiple triggers of the
    same type and have them work correctly ([issue
    \#20524](https://issues.jenkins-ci.org/browse/JENKINS-20524))
-   Fixed issue where pre-send script would get expanded and remain
    expanded ([issue
    \#20770](https://issues.jenkins-ci.org/browse/JENKINS-20770))
-   Added First Unstable trigger
-   Added helper script for emailing Jive community (thanks Dan Barker)

## 2.36 (October 26, 2013)

-   Fixed issue with mismatch between form field name and what was
    parsed in the backend ([issue
    \#20133](https://issues.jenkins-ci.org/browse/JENKINS-20133))
-   Fixed issue with NPE on 1.535 (thanks to
    [agudian](https://github.com/agudian) for the PR)
-   Added ability to customize the date format for the CHANGES\_SINCE
    tokens ([issue
    \#20151](https://issues.jenkins-ci.org/browse/JENKINS-20151))
-   Added help files for all the built-in triggers ([issue
    \#20170](https://issues.jenkins-ci.org/browse/JENKINS-20170))
-   By default when you add the email-ext publisher, an Always trigger
    will be added ([issue
    \#20013](https://issues.jenkins-ci.org/browse/JENKINS-20013))
-   Fixed issue with template project plugin based jobs and Jelly script
    content. ([issue
    \#20117](https://issues.jenkins-ci.org/browse/JENKINS-20117))
-   If user doesn't have a Mailer.UserProperty, just add the user id and
    see if the system can resolve it ([issue
    \#20215](https://issues.jenkins-ci.org/browse/JENKINS-20215))
-   Fixed issue with newInstance method for EmailTrigger causing an
    exception on 1.536 ([issue
    \#20198](https://issues.jenkins-ci.org/browse/JENKINS-20198))

## 2.35.1 (October 14, 2013)

-   Fixed issue introduced by new parameter parsing in global config
    ([issue
    \#20030](https://issues.jenkins-ci.org/browse/JENKINS-20030))
-   Update descriptor usage in triggers
-   Fixed command line to not max out PermGen during testing

## 2.35 (October 12, 2013)

-   Refactored descriptor to follow recommended method
-   Added tests for global config default values
-   Updated to latest LTS for parent version
-   Updated exclusion list to be for full email list, not just
    committers
-   Fixed issue with email-ext not restoring values for some fields if
    no triggers were configured ([issue
    \#15442](https://issues.jenkins-ci.org/browse/JENKINS-15442))
-   Fixed issue where the project could be null ([issue
    \#14338](https://issues.jenkins-ci.org/browse/JENKINS-14338))

## 2.34 (September 15, 2013)

-   Started adding HtmlUnit tests for UI interaction and round trip
    testing
-   Added more debug for excluded committers feature
-   Reverted field name to includeCulprits
-   Fixed issue where PROJECT\_DEFAULT\_RECIPIENTS was being added to
    triggers ([issue
    \#19583](https://issues.jenkins-ci.org/browse/JENKINS-19583))
-   Fixed issue that stopped users from being able to add triggers
    ([issue
    \#19585](https://issues.jenkins-ci.org/browse/JENKINS-19585))

## 2.33 (September 12, 2013)

-   Fixed issue with triggers for matrix projects not saving the value
    correctly ([issue
    \#19291](https://issues.jenkins-ci.org/browse/JENKINS-19291))
-   Added ability to set content type at the trigger level
-   Added back send to culprits
-   Fixed missing dependency for Mailer plugin in pom.xml
-   Added setting debug mode for JavaMail when debug mode for email-ext
    is set

## 2.32 (August 13, 2013)

-   Fix issue with matrix project ([issue
    \#19190](https://issues.jenkins-ci.org/browse/JENKINS-19190))
-   Added "Fixed Unhealthy" trigger

## 2.31 (August 12, 2013)

-   Changed way that triggers work so that you can add multiple triggers
    of the same type
-   Changed triggers into extension points so that other plugins can
    provide email triggers
-   Migrated all tokens to use the Token Macro plugin
-   Fixed issue with using template testing with Jelly scripts ([issue
    \#18157](https://issues.jenkins-ci.org/browse/JENKINS-18157))
-   General clean-up of sources to remove unused imports and so forth
-   Added JUnit report into groovy html template (thanks
    [praagii](https://github.com/praagii))
-   Fixed issue with wrong StringUtils being imported thanks to NetBeans
    ([issue
    \#19089](https://issues.jenkins-ci.org/browse/JENKINS-19089))

## 2.30.2 (May 23, 2013)

-   Fix issue with escaping tokens by using a more groovy like method
    (double dollar $$) ([issue
    \#18014](https://issues.jenkins-ci.org/browse/JENKINS-18014))
-   Fix issue with metaClass for Script (thanks to Geoff Cummings for
    patch) ([issue
    \#17910](https://issues.jenkins-ci.org/browse/JENKINS-17910))
-   Added ability to test Groovy and Jelly templates via a link on the
    project page. ([issue
    \#9594](https://issues.jenkins-ci.org/browse/JENKINS-9594))

## 2.29 (May 6, 2013)

-   Refixed typo in email template.
-   Updated to latest LTS release for base (1.509.1)
-   Added global user exclusion list ([issue
    \#17503](https://issues.jenkins-ci.org/browse/JENKINS-17503))
-   Added expansion of environment variable in the FILE token's path
    argument ([issue
    \#16716](https://issues.jenkins-ci.org/browse/JENKINS-16716))
-   Added trigger and triggered variables to pre-send script object
    model ([issue
    \#17577](https://issues.jenkins-ci.org/browse/JENKINS-17577))
-   Added DEFAULT\_PRESEND\_SCRIPT token ([issue
    \#14508](https://issues.jenkins-ci.org/browse/JENKINS-14508))
-   Added option to save the output of the generated email into the
    workspace ([issue
    \#13302](https://issues.jenkins-ci.org/browse/JENKINS-13302))
-   Added new trigger for broken -\> compiling state
    ([17546](https://issues.jenkins-ci.org/browse/JENKINS-17546))
-   Fixed default value for ReplyTo ([issue
    \#17733](https://issues.jenkins-ci.org/browse/JENKINS-17733))
-   Turned off pretty-printing for the CssInliner ([issue
    \#17759](https://issues.jenkins-ci.org/browse/JENKINS-17759))

## 2.28 (April 4, 2013)

-   Fixed token macro help in projects
-   Added additional Chinese translations
-   Improved help text for `BUILD_LOG_EXCERPT` token
-   Added support for inlining CSS and images into emails
-   Fixed regression in attaching build log ([issue
    \#17296](https://issues.jenkins-ci.org/browse/JENKINS-17296))
-   Fixed regression in 1st Failure Trigger ([issue
    \#17307](https://issues.jenkins-ci.org/browse/JENKINS-17307))
-   Updated docs for Improvement trigger ([issue
    \#17074](https://issues.jenkins-ci.org/browse/JENKINS-17074))
-   Fixed class loading inside Groovy templates ([issue
    \#16990](https://issues.jenkins-ci.org/browse/JENKINS-16990)))
-   Removed script that created and used template usage
-   Cleaned up unused files

## 2.27.1 (March 5, 2013)

-   Fix issue with matrix configurations ([issue
    \#17064](https://issues.jenkins-ci.org/browse/JENKINS-17064))
-   Add 1st Failure and 2nd Failure Triggers

## 2.27 (March 2, 2013)

-   Re-added ability to use tokens in attachment areas
-   Allow a default string if regex match is not found for
    BUILD\_LOG\_REGEX ([issue
    \#16269](https://issues.jenkins-ci.org/browse/JENKINS-16269))
-   Fixed message layout if attachments are present ([issue
    \#16281](https://issues.jenkins-ci.org/browse/JENKINS-16281))
-   Added info to the help on using the CC: mechanism
-   Fixed an issue with regression triggers ([issue
    \#16404](https://issues.jenkins-ci.org/browse/JENKINS-16404))
-   Added a single retry if a SocketException occurs, in case the
    network issue was temporary ([issue
    \#16181](https://issues.jenkins-ci.org/browse/JENKINS-16181))
-   Fixed attaching build log from a trigger.
-   Made default send to lists less verbose for certain triggers ([issue
    \#8642](https://issues.jenkins-ci.org/browse/JENKINS-8642))
-   Added support for personal portions of email addresses ("Some Name"
    \<username@email.com\>) including support for unicode
-   Added check of return values from SendFailedException ([issue
    \#16919](https://issues.jenkins-ci.org/browse/JENKINS-16919))
-   Made it much easier to use content tokens from groovy templates
    ([issue
    \#16916](https://issues.jenkins-ci.org/browse/JENKINS-16916))
-   Fixed a typo in the html template ([issue
    \#16975](https://issues.jenkins-ci.org/browse/JENKINS-16975))
-   Fixed groovy html template when Maven artifacts cause an exception
    ([issue
    \#16983](https://issues.jenkins-ci.org/browse/JENKINS-16983))
-   Include Jacoco output in the default Jelly HTML template.

## 2.25 (December 12, 2012)

-   Fixed test failures on Mac OS
-   Fixed issue with NullReferenceException if the file doesn't exist
    for the FILE token ([issue
    \#15008](https://issues.jenkins-ci.org/browse/JENKINS-15008))
-   Improved address resolution if the user is setup in the Jenkins
    system
-   Added a debug mode that will add extra log messages to the build log
    when enabled in the global config.
-   Updated to core 1.480
-   Added ability to add attachments at the trigger level ([issue
    \#13672](https://issues.jenkins-ci.org/browse/JENKINS-13672))
-   Added option to attach the build log at either the project level, or
    at the trigger level ([issue
    \#13848](https://issues.jenkins-ci.org/browse/JENKINS-13848))
-   Improved capture of failed email addresses ([issue
    \#16076](https://issues.jenkins-ci.org/browse/JENKINS-16076))
-   Added ability to set Reply-To header value at global, project and
    trigger level. ([issue
    \#3324](https://issues.jenkins-ci.org/browse/JENKINS-3324))\* Added
    ability to set Reply-To header value at global, project and trigger
    level. ([issue
    \#3324](https://issues.jenkins-ci.org/browse/JENKINS-3324))
-   Added parameter (maxLength) to FAILED\_TESTS content token to allow
    truncating the test information. The maxLength is the number of KB
    allowed ([issue
    \#5949](https://issues.jenkins-ci.org/browse/JENKINS-5949))\* Added
    parameter (maxLength) to FAILED\_TESTS content token to allow
    truncating the test information. The maxLength is the number of KB
    allowed ([issue
    \#5949](https://issues.jenkins-ci.org/browse/JENKINS-5949))
-   Added ability to secure the pre-send script by adding a sandbox when
    enabled in the global config. ([issue
    \#15213](https://issues.jenkins-ci.org/browse/JENKINS-15213))

## 2.24.1 (July 20, 2012)

-   Fixed a few tests which were erroring on Windows.
-   Fixed issue with very long token strings causing SOE ([issue
    \#14132](https://issues.jenkins-ci.org/browse/JENKINS-14132))
-   Updated TEST\_COUNTS token to include passing tests.
-   Fixed charset issue when using Jelly templates ([issue
    \#7997](https://issues.jenkins-ci.org/browse/JENKINS-7997))
-   Allow nested content in JELLEY\_SCRIPT tag ([issue
    \#14210](https://issues.jenkins-ci.org/browse/JENKINS-14210))
-   Added onlyRegressions parameter to FAILED\_TESTS token
-   Allow disable of newlines after each regex match ([issue
    \#14320](https://issues.jenkins-ci.org/browse/JENKINS-14320))
-   Removed token macro error messages from logs ([issue
    \#9364](https://issues.jenkins-ci.org/browse/JENKINS-9364))
-   Fixed issue when token-macro was older than expected ([issue
    \#14224](https://issues.jenkins-ci.org/browse/JENKINS-14224))
-   Fixed changeset author issue with text template
-   Added new trigger for when a job first fails ([issue
    \#7859](https://issues.jenkins-ci.org/browse/JENKINS-7859))
-   Allow specifying CC addresses ([issue
    \#6703](https://issues.jenkins-ci.org/browse/JENKINS-6703))
-   Updated improvement trigger to only fire if there are failures, but
    less than previous build ([issue
    \#14500](https://issues.jenkins-ci.org/browse/JENKINS-14500))

## 2.22 (June 15, 2012)

-   Added pre-send groovy script for modifying the MimeMessage and even
    cancelling the email altogether. ([issue
    \#12421](https://issues.jenkins-ci.org/browse/JENKINS-12421))
-   Added support for the token-macro plugin ([issue
    \#9364](https://issues.jenkins-ci.org/browse/JENKINS-9364))
-   Added try/catch around user email resolution ([issue
    \#13102](https://issues.jenkins-ci.org/browse/JENKINS-13102))
-   Attachment file path now supports content tokens ([issue
    \#13563](https://issues.jenkins-ci.org/browse/JENKINS-13563))
-   Fixed issues with tests causing OutOfMemory exception
-   Added `BUILD_LOG_MULTILINE_REGEX` that allows regexes to match even
    newlines

## 2.21 (May 16, 2012)

-   Fix issue with new drop down list for post-build ([issue
    \#13737](https://issues.jenkins-ci.org/browse/JENKINS-13737))
-   Added a [new jelly
    template](https://github.com/jenkinsci/email-ext-plugin/blob/master/src/main/resources/hudson/plugins/emailext/templates/static-analysis.jelly)
    that shows the [static analysis results](https://plugins.jenkins.io/analysis-core/)

## 2.20 (April 12, 2012)

-   Fix issue with hierarchical projects
    (see \[[Hierarchical+projects+support](https://wiki.jenkins.io/display/JENKINS/Hierarchical+projects+support)\|\])
-   Updated html\_gmail.jelly file to updated fields
-   Updated maven pom to use repo.jenkins-ci.org repository
-   Added scripts for regenerating html.jelly for inline CSS styles
-   Fix issue with Jenkins URL overriding ([issue
    \#13242](https://issues.jenkins-ci.org/browse/JENKINS-13242))
-   Fix groovy template for git usage ([issue
    \#13192](https://issues.jenkins-ci.org/browse/JENKINS-13192))
-   Fix NPE that causes build to hang ([issue
    \#12577](https://issues.jenkins-ci.org/browse/JENKINS-12577))

## 2.19 ( Mar 24, 2012 )

-   Reimplement default (global) recipient list
-   Fixed default suffix lookup ([issue
    \#11731](https://issues.jenkins-ci.org/browse/JENKINS-11731))
-   Added JOB\_DESCRIPTION token ([issue
    \#4100](https://issues.jenkins-ci.org/browse/JENKINS-4100))
-   Added BUILD\_ID token ([issue
    \#11895](https://issues.jenkins-ci.org/browse/JENKINS-11895))
-   Groovy template now correctly determines SUCCESS and FAILURE ([issue
    \#13191](https://issues.jenkins-ci.org/browse/JENKINS-13191))
-   CHANGES now allows nested content ([issue
    \#5376](https://issues.jenkins-ci.org/browse/JENKINS-5376))
-   Fixed NRE when recipientList is not in the saved config ([issue
    \#12047](https://issues.jenkins-ci.org/browse/JENKINS-12047))
-   Emails now send when one or more of the recipients is an invalid
    recipient ([issue
    \#9006](https://issues.jenkins-ci.org/browse/JENKINS-9006))
-   Fixed issues with default recipients ([issue
    \#11665](https://issues.jenkins-ci.org/browse/JENKINS-11665))

## 2.18 ( Jan 31, 2012 )

-   Add maximum size limit to ${FAILED\_TESTS}
    ([JENKINS-11413](https://issues.jenkins-ci.org/browse/JENKINS-11413))
-   Added improvement and regression triggers
-   Added ${BUILD\_LOG\_EXCERPT} token ([issue
    \#10924](https://issues.jenkins-ci.org/browse/JENKINS-10924))
-   Added emergency reroute option
-   Made compatible with LTS 1.424
-   Email to requester is now correct ([issue
    \#9160](https://issues.jenkins-ci.org/browse/JENKINS-9160))
-   Fixed configuration with promoted builds ([issue
    \#10812](https://issues.jenkins-ci.org/browse/JENKINS-10812))
-   Only include the stack trace if showStacks is true ([issue
    \#3430](https://issues.jenkins-ci.org/browse/JENKINS-3430))

## 2.16 (Nov 07, 2011)

-   More flexible firing control for matrix projects
    ([JENKINS-8590](https://issues.jenkins-ci.org/browse/JENKINS-8590))
-   E-mail trigger for aborted and "not built" results
    ([JENKINS-10990](https://issues.jenkins-ci.org/browse/JENKINS-10990))

## 2.15 (Sep 05, 2011)

-   Allow email-ext to attach files to emails
    ([JENKINS-9018](https://issues.jenkins-ci.org/browse/JENKINS-9018)).
-   Default Recipients list does not appear in Jenkins global
    settings([JENKINS-10783](https://issues.jenkins-ci.org/browse/JENKINS-10783)).
-   Email to requester uses wrong email address
    ([JENKINS-9160](https://issues.jenkins-ci.org/browse/JENKINS-9160)).
-   Allow using Groovy (or other JSR223 languages) to generate the email
    content.

## 2.14.1 (Jul 01, 2011)

-   Added option for adding 'Precedence: bulk' header according to
    <http://tools.ietf.org/search/rfc3834> to prevent out-of-office
    replies.

## 2.14 (Apr 21, 2011)

-   Improved the portability of the default Jelly templates across
    different SCM implementations (whereas previously some of the
    information was only displayed for Subversion)
-   Send the "still unstable" email rather than the "unstable" email,
    when the previous status was fail, and the status before that was
    unstable.
    ([JENKINS-5411](https://issues.jenkins-ci.org/browse/JENKINS-5411))

## 2.13 (Mar 23 2011)

-   Fixed a bug where the html/text Jelly template fail to report the
    change log correctly for all SCMs but Subversion.
-   If an e-mail is supposed to be sent to the requester, follow the
    build triggering chain to find the root requester
    ([JENKINS-7740](https://issues.jenkins-ci.org/browse/JENKINS-7740))
-   Added an option to configure a List-ID header on emails.

## 2.12 (Feb 26, 2011)

-   Rerelease 2.11 to properly set required Jenkins version.

## 2.11 (Feb 19, 2011)

**This version requires Jenkins 1.396 or newer.**

-   Added Charset option.
    ([JENKINS-8011](https://issues.jenkins-ci.org/browse/JENKINS-8011))
    -   Added
        "hudson.plugins.emailext.ExtendedEmailPublisher.Content-Transfer-Encoding"
        system property to specify "Content-Transfer-Encoding".
-   Added "Requester" as possible mail destination
    ([JENKINS-7740](https://issues.jenkins-ci.org/browse/JENKINS-7740))
-   Need tokens to get failed tests count and total tests count, to put
    them in mail subject easy.
    ([JENKINS-5936](https://issues.jenkins-ci.org/browse/JENKINS-5936))
-   Introduce $JENKINS\_URL and deprecated $HUDSON\_URL.
-   i18n & l10n(ja)

## 2.10 (Jan 20, 2011)

-   Added a new content token "FILE" that includes a file from the
    workspace.
-   BUILD\_LOG\_REGEX Token:
    -   Add escapeHtml - If true, escape HTML. Defaults to false.
    -   Add matchedLineHtmlStyle - If non-null, output HTML. Matched
        lines will become
        `<b style="your-style-value">html escaped matched line</b>`.
        Defaults to null.
-   Prevent duplicate email notifications.
    ([JENKINS-8071](https://issues.jenkins-ci.org/browse/JENKINS-8071))

## 2.9 (Oct 14, 2010)

-   The *showPaths* argument was not working for
    CHANGES\_SINCE\_LAST\_UNSTABLE and CHANGES\_SINCE\_LAST\_SUCCESS.
    (issue \#[5486](http://issues.jenkins-ci.org/browse/JENKINS-5486))
-   Add support for custom Jelly script content (JELLY\_SCRIPT) (issue
    \#[7514](http://issues.jenkins-ci.org/browse/JENKINS-7514))

## 2.8 (Sept 15, 2010)

*This version requires Hudson 1.356 or newer.*

-   Update BUILD\_LOG\_REGEX to properly handle [console
    notes](http://kohsuke.org/2010/04/14/hudson-console-markups/).
    (issue \#[7402](http://issues.jenkins-ci.org/browse/JENKINS-7402))
-   Fixed password being saved in plaintext. (issue
    \#[5816](http://issues.jenkins-ci.org/browse/JENKINS-5816))
-   Override "Hudson URL" only when "override global settings" is
    checked. (issue
    \#[6193](http://issues.jenkins-ci.org/browse/JENKINS-6193))
-   Add escapeHtml parameter to BUILD\_LOG content for escaping HTML.
    Defaults to false for backwards compatibility. (issue
    \#[7397](http://issues.jenkins-ci.org/browse/JENKINS-7397))

## 2.7 (Aug 30, 2010)

-   New optional arg: ${BUILD\_LOG\_REGEX, regex, linesBefore,
    linesAfter, maxMatches, showTruncatedLines, substText} which allows
    substituting text for the matched regex. This is particularly useful
    when the text contains references to capture groups (i.e. $1, $2,
    etc.)

&nbsp;

-   Fix invalid illegal email address exception
    ([JENKINS-7057](https://issues.jenkins-ci.org/browse/JENKINS-7057)).

## 2.6 (Jul 20, 2010)

-   Add ${BUILD\_LOG\_REGEX, regex, linesBefore, linesAfter, maxMatches,
    showTruncatedLines} token.
-   Add token for build cause.
    ([JENKINS-3166](https://issues.jenkins-ci.org/browse/JENKINS-3166))
-   Add "changes since last unstable build" token.
    ([JENKINS-6671](https://issues.jenkins-ci.org/browse/JENKINS-6671))
-   Fix issue with node properties not being available for the $ENV
    token.
    ([JENKINS-5465](https://issues.jenkins-ci.org/browse/JENKINS-5465))
-   Recipient list can now use parameters.
    ([JENKINS-6396](https://issues.jenkins-ci.org/browse/JENKINS-6396))
-   Improve docs regarding use of quotes for string parameters.
    ([JENKINS-5322](https://issues.jenkins-ci.org/browse/JENKINS-5322))

## 2.5 (Jan 20, 2010)

-   Fix issue with adding a pre-build trigger using $BUILD\_STATUS would
    make the build appear as if it was successful or fixed when the
    build hadn't actually ran yet. ([issue
    \#953](http://issues.jenkins-ci.org/browse/JENKINS-953))
-   Fix NullPointerException when no root URL is configured. ([issue
    \#1771](http://issues.jenkins-ci.org/browse/JENKINS-1771))
-   $CHANGES\_SINCE\_LAST\_SUCCESS was not showing unstable or aborted
    builds in the list of changes since the last successful build.
    ([issue \#3519](http://issues.jenkins-ci.org/browse/JENKINS-3519))

## 2.4 (Jan 7, 2010)

-   Fix bug in 2.3 release that broke saving project config changes if
    Promoted Builds plugin is not also installed.
    ([JENKINS-5208](https://issues.jenkins-ci.org/browse/JENKINS-5208))
-   Fix in overriding global email settings.
-   Fix to allow authentication without SSL.
-   Send emails as replies to previous ones for same project, as done in
    Hudson's built-in emailer.
    ([JENKINS-3089](https://issues.jenkins-ci.org/browse/JENKINS-3089))
-   New "Before Build" trigger type.
    ([JENKINS-4190](https://issues.jenkins-ci.org/browse/JENKINS-4190))

## 2.3 (Jan 6, 2010)

-   Change the token handling to allow for passing of arguments, and
    allow arguments for the `BUILD_LOG`, `CHANGES`, and
    `CHANGES_SINCE_LAST_SUCCESS` tokens.
    ([JENKINS-3085](https://issues.jenkins-ci.org/browse/JENKINS-3085))
-   Revamp the help. Now have help on each form element in the config.
    Rearranged help files hierarchially and deleted unused help.
-   Allow HTML content in emails. There is a global preference plus a
    per-project preference, which default to plain text.
-   When the emailer can not process an email address, it now prints to
    the builder output.
    ([JENKINS-1529](https://issues.jenkins-ci.org/browse/JENKINS-1529))
-   Allow use of any environment variable.
    ([JENKINS-3605](https://issues.jenkins-ci.org/browse/JENKINS-3605))
-   Add ability to re-use "global" settings (i.e. settings from the core
    Mailer configuration)
-   Add support for SVN\_REVISION
-   Fix for email triggers with space in name.
    ([JENKINS-3614](https://issues.jenkins-ci.org/browse/JENKINS-3614))
-   Update code for more recent Hudson.
-   Fixed help links.
    ([JENKINS-4566](https://issues.jenkins-ci.org/browse/JENKINS-4566))
-   Compatibility with [Promoted Builds Plugin](https://plugins.jenkins.io/promoted-builds/).

## 2.2.1 (Dec 23, 2008)
