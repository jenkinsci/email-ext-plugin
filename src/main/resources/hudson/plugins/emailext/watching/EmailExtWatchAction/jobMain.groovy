package hudson.plugins.emailext.watching.EmailExtWatchAction
// Namespaces
def t = namespace("/lib/hudson")

import hudson.model.User

if(app.getDescriptor('hudson.plugins.emailext.ExtendedEmailPublisher').watchingEnabled && User.current() != null) {
    table(style: "margin-top: 1em; margin-left:1em;") {
        t.summary(icon: "/plugin/email-ext/images/add-watch.svg", href: my.urlName, permission: my.project.READ, my.displayName)
    }
}

