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
        script """function onSubmit() {
                var templateFile = document.getElementById('template_file_name').value;
                var buildId = document.getElementById('template_build').value;
                templateTester.renderTemplate(templateFile,buildId, function(t) {
                    document.getElementById('rendered_template').innerHTML = t.responseObject()[0];
                    var consoleOutput = t.responseObject()[1];
                    if(consoleOutput.length == 0) {
                        document.getElementById('output').style.display = 'none';                        
                    } else {
                        document.getElementById('output').style.display = 'block';
                        document.getElementById('console_output').innerHTML = consoleOutput;
                    }
                });
                return false;
            }"""
        h1(my.displayName)        
        if(hasPermission) {
            h3(_("description"))
            form(action: "", method: "post", name: "templateTest", onSubmit: "return onSubmit();") {
                table {
                    f.entry(title: _("Jelly/Groovy Template File Name")) {
                        f.textbox(name: "template_file_name", id: "template_file_name", clazz: "required", checkUrl:"'templateFileCheck?value='+this.value")
                    }
                    f.entry(title: _("Build To Test")) {
                        select(name: "template_build", id: "template_build") {
                            my.project.builds.each { build ->
                                f.option(value: build.id, "#${build.number} (${build.result})")
                            }
                        }
                    }
                    f.entry {
                        f.submit(value: _("Go!"))
                    }
                }
            }
            div(id: "rendered_template")
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