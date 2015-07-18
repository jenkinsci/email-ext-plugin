package hudson.plugins.emailext.watching.EmailExtWatchAction

// Namespaces
def l = namespace(lib.LayoutTagLib)
def f = namespace(lib.FormTagLib)
def st = namespace("jelly:stapler")

import hudson.Functions
import hudson.matrix.MatrixProject
import hudson.model.User

def descriptor = app.getDescriptor("hudson.plugins.emailext.ExtendedEmailPublisher")
def watchEnabled = descriptor.watchingEnabled
def hasPermission = hudson.Functions.hasPermission(my.project, my.project.READ)
def user = User.current()

// we only want logged in users to be able to watch jobs
if(user==null || !watchEnabled) {
    st.redirect(url: "${my.project.absoluteUrl}")
}

def mailerProperty = it.mailerProperty

l.layout(norefresh: true) {
    st.include(it: my.project, page: "sidepanel")
    l.main_panel {
        h1(my.displayName)        
        if(hasPermission) {
            h3(_("description"))
            
            if(mailerProperty==null) {
                p(_("You currently do not have an email address setup for your user account. You cannot watch jobs until you set an email address"))
            } else {
                
                if(my?.triggers?.size() > 0) {
                    f.form(method: "post", action: "stopWatching", name: "stopWatching") {
                        f.block {
                            f.submit(value:_("Stop Watching"))
                        }
                    }
                    br()
                }

                f.form(method: "post", action: "configSubmit", name: "config") {

                    f.entry(title: _("Content Token Reference"), help: descriptor.getHelpFile('tokens'))
                    def triggers = hudson.plugins.emailext.plugins.EmailTrigger.all().findAll { t -> t.isWatchable() }
                    def configuredTriggers = (my != null && my.triggers.size() > 0) ? my.triggers : []
                    // do we want to filter the triggers somehow so that only some show up?

                    showSendTo = false
                    f.entry(field:"triggers", title: _("Triggers")) {
                      f.hetero_list(name: "triggers", hasHeader: true, descriptors: triggers, items: configuredTriggers, addCaption:_("Add Trigger"), deleteCaption: _("Remove Trigger"), capture: "showSendTo")
                    }

                    f.block {
                      f.submit(value:_("Submit"))
                    }

                }
            }
        } else {
            st.redirect(url: "${my.project.absoluteUrl}")
        }
    }
}