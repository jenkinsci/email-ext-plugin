package hudson.plugins.emailext;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnamePortRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import jenkins.security.FIPS140;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class MailAccount extends AbstractDescribableImpl<MailAccount> {
    private String address;
    private String smtpHost;
    private String smtpPort = "25";
    private transient String smtpUsername;
    private transient Secret smtpPassword;
    private String credentialsId;
    private boolean useSsl;
    private boolean useTls;
    private String advProperties;
    private boolean defaultAccount;

    private boolean useOAuth2;

    @Deprecated
    public MailAccount(JSONObject jo) {
        address = Util.nullify(jo.optString("address", null));
        smtpHost = Util.nullify(jo.optString("smtpHost", null));
        smtpPort = Util.nullify(jo.optString("smtpPort", null));
        if (jo.optBoolean("auth", false)) {
            credentialsId = Util.nullify(jo.optString("credentialsId", null));
        }
        useSsl = jo.optBoolean("useSsl", false);
        useTls = jo.optBoolean("useTls", false);
        advProperties = Util.nullify(jo.optString("advProperties", null));
    }

    @DataBoundConstructor
    public MailAccount() {}

    public boolean isValid() {
        return isFromAddressValid() && isSmtpServerValid() && isSecureAuthWhenFIPS();
    }

    public boolean isSecureAuthWhenFIPS() {
        // when in FIPS mode if we are using authentication we must also use TLS or SSL to protect the password
        return !(credentialsId != null && FIPS140.useCompliantAlgorithms() && !(useSsl || useTls));
    }

    public boolean isFromAddressValid() {
        return isDefaultAccount() || StringUtils.isNotBlank(address);
    }

    public boolean isSmtpServerValid() {
        if (StringUtils.isBlank(smtpHost)) {
            return true; // empty means localhost
        }
        try {
            InetAddress.getByName(smtpHost);
            return true; // resolved successfully (IPv4, IPv6, or hostname)
        } catch (UnknownHostException e) {
            // Offline or unresolvable – fall back to syntax checks
            return isSyntacticallyValid(smtpHost);
        }
    }

    private boolean isSyntacticallyValid(String host) {
        // Check if it looks like an IPv4 address (four dot‑separated numbers)
        Pattern ipv4Structure = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");
        if (ipv4Structure.matcher(host).matches()) {
            // Strict IPv4 validation
            Pattern ipv4Valid = Pattern.compile(
                    "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            return ipv4Valid.matcher(host).matches();
        }
        // Otherwise treat as hostname (RFC 1123)
        Pattern hostnameValid =
                Pattern.compile("^(?![0-9]+$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$");
        return hostnameValid.matcher(host).matches() && host.length() <= 255;
    }

    public boolean isSmtpAuthValid() {
        return true;
        // Note: we either have credentials or not, there isn't
        // two pieces to look at
    }

    public boolean isDefaultAccount() {
        return defaultAccount;
    }

    @DataBoundSetter
    void setDefaultAccount(boolean defaultAccount) {
        this.defaultAccount = defaultAccount;
    }

    @Override
    public MailAccountDescriptor getDescriptor() {
        return (MailAccountDescriptor) Jenkins.get().getDescriptor(getClass());
    }

    @Extension
    public static class MailAccountDescriptor extends Descriptor<MailAccount> {
        @NonNull
        @Override
        public String getDisplayName() {
            return "";
        }

        @SuppressWarnings({"lgtm[jenkins/csrf]", "unused"}) // Used by stapler
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {

            final StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            return result.includeEmptyValue()
                    .includeMatchingAs(
                            item instanceof Queue.Task t ? Tasks.getAuthenticationOf(t) : Jenkins.getAuthentication(),
                            item,
                            StandardUsernamePasswordCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class))
                    .includeCurrentValue(credentialsId);
        }

        @SuppressWarnings("lgtm[jenkins/csrf]")
        public FormValidation doCheckCredentialsId(
                @AncestorInPath Item item,
                @QueryParameter String value,
                @QueryParameter boolean useSsl,
                @QueryParameter boolean useTls) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }
            if (StringUtils.isBlank(value)) {
                return FormValidation.ok();
            }

            // do this after the authentication check so we do not reveal the FIPS mode of the controller.
            FormValidation insecureAuthValidation;
            if (useSsl || useTls) {
                insecureAuthValidation = FormValidation.ok();
            } else {
                if (FIPS140.useCompliantAlgorithms()) {
                    insecureAuthValidation =
                            FormValidation.error("Authentication requires either TLS or SSL to be enabled");
                } else {
                    insecureAuthValidation = FormValidation.warning(
                            "For security when using authentication it is recommended to enable either TLS or SSL");
                }
            }

            if (CredentialsProvider.listCredentials(
                            StandardUsernamePasswordCredentials.class,
                            item,
                            item instanceof Queue.Task t ? Tasks.getAuthenticationOf(t) : Jenkins.getAuthentication(),
                            null,
                            CredentialsMatchers.withId(value))
                    .isEmpty()) {
                if (insecureAuthValidation.kind == Kind.OK) {
                    return FormValidation.error("Cannot find currently selected credentials");
                }
                return FormValidation.aggregate(List.of(
                        insecureAuthValidation, FormValidation.error("Cannot find currently selected credentials")));
            }
            return insecureAuthValidation;
        }
    }

    public String getAddress() {
        return address;
    }

    @DataBoundSetter
    public void setAddress(String address) {
        this.address = Util.fixEmptyAndTrim(address);
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    @DataBoundSetter
    public void setSmtpHost(String smtpHost) {
        this.smtpHost = Util.fixEmptyAndTrim(smtpHost);
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    @DataBoundSetter
    public void setSmtpPort(String smtpPort) {
        smtpPort = Util.fixEmptyAndTrim(smtpPort);

        if (smtpPort != null) {
            try {
                int port = Integer.parseInt(smtpPort);
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("SMTP port must be between 1 and 65535");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("SMTP port must be a valid number");
            }
        }

        this.smtpPort = smtpPort;
    }

    @Deprecated
    public String getSmtpUsername() {
        return smtpUsername;
    }

    @DataBoundSetter
    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = Util.fixEmptyAndTrim(smtpUsername);
    }

    @Deprecated
    public Secret getSmtpPassword() {
        return smtpPassword;
    }

    @DataBoundSetter
    public void setSmtpPassword(Secret smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = Secret.fromString(smtpPassword);
    }

    public String getCredentialsId() {
        if (StringUtils.isBlank(credentialsId) && StringUtils.isNotBlank(smtpUsername) && smtpPassword != null) {
            migrateCredentials();
        }
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    @DataBoundSetter
    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public boolean isUseTls() {
        return useTls;
    }

    @DataBoundSetter
    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public boolean isUseOAuth2() {
        return useOAuth2;
    }

    @DataBoundSetter
    public void setUseOAuth2(boolean useOAuth2) {
        this.useOAuth2 = useOAuth2;
    }

    public String getAdvProperties() {
        return advProperties;
    }

    @DataBoundSetter
    public void setAdvProperties(String advProperties) {
        this.advProperties = advProperties;
    }

    private Object readResolve() {
        if (StringUtils.isBlank(credentialsId) && StringUtils.isNotBlank(smtpUsername) && smtpPassword != null) {
            migrateCredentials();
        }
        return this;
    }

    private void migrateCredentials() {
        DomainRequirement domainRequirement = null;
        if (StringUtils.isNotBlank(smtpHost) && StringUtils.isNotBlank(smtpPort)) {
            domainRequirement = new HostnamePortRequirement(smtpHost, Integer.parseInt(smtpPort));
        }
        final List<StandardUsernamePasswordCredentials> credentials = CredentialsMatchers.filter(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        StandardUsernamePasswordCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM2,
                        Collections.singletonList(domainRequirement)),
                CredentialsMatchers.withUsername(smtpUsername));
        for (final StandardUsernamePasswordCredentials cred : credentials) {
            if (smtpPassword.getPlainText().equals(Secret.toString(cred.getPassword()))) {
                // If some credentials have the same username/password, use those.
                credentialsId = cred.getId();
                break;
            }
        }
        if (StringUtils.isBlank(credentialsId)) {
            // If we couldn't find any existing credentials,
            // create new credentials with the principal and secret and use it.
            final StandardUsernamePasswordCredentials newCredentials;
            try {
                newCredentials = new UsernamePasswordCredentialsImpl(
                        CredentialsScope.GLOBAL,
                        null,
                        "Migrated from email-ext username/password",
                        smtpUsername,
                        smtpPassword.getPlainText());
            } catch (Descriptor.FormException e) {
                // safe to ignore as too short password should happen only in FIPS mode
                // and migrating from no FIPS to FIPS is not supported, but only fresh start
                throw new RuntimeException("Password used for email-ext server configuration could not be migrated", e);
            }
            SystemCredentialsProvider.getInstance().getCredentials().add(newCredentials);
            credentialsId = newCredentials.getId();
        }

        smtpUsername = null;
        smtpPassword = null;
    }
}
