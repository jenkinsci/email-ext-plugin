// Namespaces
f = namespace("/lib/form")
c = namespace("/lib/credentials")

if(instance?.defaultAccount) {
    f.invisibleEntry {
        f.textbox(field: "address")
    }
} else {
    f.entry(field: "address", title: _("Admin Account Address")) {
        f.textbox()
    }
}

f.entry(field: "smtpHost", title: _("SMTP server")) {
    f.textbox()
}
f.entry(field: "smtpPort", title: _("SMTP Port")) {
    f.number(default: "25")
}

f.advanced {
    f.entry(field: "credentialsId", title: _("Credentials")) {
        c.select()
    }
    f.entry(field: "useSsl", title: _("Use SSL")) {
        f.checkbox()
    }
    f.entry(field: "useTls", title: _("Use TLS")) {
        f.checkbox()
    }
    f.entry(field: "useOAuth2", title: _("Use OAuth 2.0")) {
        f.checkbox()
    }
    f.entry(field: "oauth2Provider", title: _("OAuth 2.0 Provider")) {
        f.select()
    }
    f.entry(field: "tenantId", title: _("Tenant ID")) {
        f.textbox()
    }
    f.entry(field: "advProperties", title: _("Advanced Email Properties")) {
        f.textarea()
    }
}
