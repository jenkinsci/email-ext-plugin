def l = namespace(lib.LayoutTagLib)

def requiresAdmin = app.getDescriptor('hudson.plugins.emailext.ExtendedEmailPublisher').adminRequiredForTemplateTesting
def hasPermission = requiresAdmin ? h.hasPermission(app.MANAGE) : h.hasPermission(this.action.project, this.action.project.CONFIGURE)

if(hasPermission) {
    l.task(icon: "/plugin/email-ext/images/template-debugger.svg", title: this.action.displayName,
        href: h.getActionUrl(my.url, this.action), contextMenu: h.isContextMenuVisible(this.action))
}
