// Namespaces
def l = namespace(lib.LayoutTagLib)
def j = namespace("jelly:core")
def f = namespace(lib.FormTagLib)
def st = namespace("jelly:stapler")
def t = namespace("/lib/hudson")
def d = namespace("jelly:define")

import hudson.Functions

def requiresAdmin = app.getDescriptor("hudson.plugins.emailext.ExtendedEmailPublisher").adminRequiredForTemplateTesting
def hasPermission = requiresAdmin ? hudson.Functions.hasPermission(app.MANAGE) : hudson.Functions.hasPermission(it.project, it.project.CONFIGURE);
l.layout {
    st.include(it: my.project, page: "sidepanel")
    l.main_panel {
        st.bind(var: "templateTester", value: my)
        st.adjunct(includes: "hudson.plugins.emailext.EmailExtTemplateAction.template-test")
        h1(my.displayName)        
        if(hasPermission) {
            h3(_("description"))
            form(action: "", method: "post", name: "templateTest", class: "test-template-form") {
                table {
                    f.entry(title: _("Jelly/Groovy Template File Name")) {
                        f.textbox(name: "template_file_name", id: "template_file_name", clazz: "required", checkUrl:"templateFileCheck", checkDependsOn: "")
                    }
                    f.entry(title: _("Build To Test")) {
                        div(class: "jenkins-select") {
                            select(name: "template_build", id: "template_build", class: "jenkins-select__input") {
                                my.project.builds.each { build ->
                                    f.option(value: build.id, "#${build.number} (${build.result})")
                                }
                            }
                        }
                    }
                    f.entry {
                        f.submit(value: _("Go!"))
                    }
                }
            }
            iframe(id:"rendered_template", width:"80%", height:"500px", frameBorder:"0", sandbox:"")
            div(id: "output", style: "display:none;") {
                hr()
                h3(_("Template Console Output"))
                pre(id: "console_output", clazz: "console-output")
            }
        } else {
            // redirect to the root in the case that someone tries to do
            // bad stuff...
            st.redirect(url: "${rootURL}")
        }
    }
}
