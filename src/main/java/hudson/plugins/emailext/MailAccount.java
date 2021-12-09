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
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.Collections;
import java.util.List;

public class MailAccount extends AbstractDescribableImpl<MailAccount>{
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

    @Deprecated
    public MailAccount(JSONObject jo){
        address = Util.nullify(jo.optString("address", null));
        smtpHost = Util.nullify(jo.optString("smtpHost", null));
        smtpPort = Util.nullify(jo.optString("smtpPort", null));
        if(jo.optBoolean("auth", false)){
            credentialsId = Util.nullify(jo.optString("credentialsId", null));
        }
        useSsl = jo.optBoolean("useSsl", false);
        useTls = jo.optBoolean("useTls", false);
        advProperties = Util.nullify(jo.optString("advProperties", null));
    }

    @DataBoundConstructor
    public MailAccount() {

    }

    public boolean isValid() {
        return isFromAddressValid() && isSmtpServerValid();
    }

    public boolean isFromAddressValid() {
        return isDefaultAccount() || StringUtils.isNotBlank(address);
    }

    public boolean isSmtpServerValid() {
        return true;
        // Note: having no SMTP server is fine, it means localhost.
        // More control could be implemented here when not null though,
        // like checking the value looks like an FQDN or IP address.
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

    public MailAccountDescriptor getDescriptor() {
        return (MailAccountDescriptor) Jenkins.get().getDescriptor(getClass());
    }

    @Extension
    public static class MailAccountDescriptor extends Descriptor<MailAccount>{
        @NonNull
        @Override
        public String getDisplayName(){
            return "";
        }

        @SuppressWarnings("unused") // Used by stapler
        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item item,
                @QueryParameter String credentialsId) {

            final StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            return result
                    .includeEmptyValue()
                    .includeMatchingAs(
                            item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM,
                            item,
                            StandardUsernamePasswordCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class))
                    .includeCurrentValue(credentialsId);
        }

        public FormValidation doCheckCredentialsId(
                @AncestorInPath Item item,
                @QueryParameter String value) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }
            if (StringUtils.isBlank(value)) {
                return FormValidation.ok();
            }

            if (CredentialsProvider.listCredentials(
                    StandardUsernamePasswordCredentials.class,
                    item,
                    item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task)item) : ACL.SYSTEM,
                    null,
                    CredentialsMatchers.withId(value)).isEmpty()) {
                return FormValidation.error("Cannot find currently selected credentials");
            }
            return FormValidation.ok();
        }
    }

    public String getAddress(){
        return address;
    }

    @DataBoundSetter
    public void setAddress(String address){
        this.address = Util.fixEmptyAndTrim(address);
    }

    public String getSmtpHost(){
        return smtpHost;
    }

    @DataBoundSetter
    public void setSmtpHost(String smtpHost){
        this.smtpHost = Util.fixEmptyAndTrim(smtpHost);
    }

    public String getSmtpPort(){
        return smtpPort;
    }

    @DataBoundSetter
    public void setSmtpPort(String smtpPort){
        this.smtpPort = Util.fixEmptyAndTrim(smtpPort);
    }

    @Deprecated
    public String getSmtpUsername(){
        return smtpUsername;
    }

    @DataBoundSetter
    public void setSmtpUsername(String smtpUsername){
        this.smtpUsername = Util.fixEmptyAndTrim(smtpUsername);
    }

    @Deprecated
    public Secret getSmtpPassword(){
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
        if(StringUtils.isBlank(credentialsId) && StringUtils.isNotBlank(smtpUsername) && smtpPassword != null) {
            migrateCredentials();
        }
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    }

    public boolean isUseSsl(){
        return useSsl;
    }

    @DataBoundSetter
    public void setUseSsl(boolean useSsl){
        this.useSsl = useSsl;
    }

    public boolean isUseTls(){
        return useTls;
    }

    @DataBoundSetter
    public void setUseTls(boolean useTls){
        this.useTls = useTls;
    }

    public String getAdvProperties(){
        return advProperties;
    }

    @DataBoundSetter
    public void setAdvProperties(String advProperties){
        this.advProperties = advProperties;
    }

    private Object readResolve() {
        if(StringUtils.isBlank(credentialsId) && StringUtils.isNotBlank(smtpUsername) && smtpPassword != null) {
            migrateCredentials();
        }
        return this;
    }

    private void migrateCredentials() {
        DomainRequirement domainRequirement = null;
        if(StringUtils.isNotBlank(smtpHost) && StringUtils.isNotBlank(smtpPort)) {
            domainRequirement = new HostnamePortRequirement(smtpHost, Integer.parseInt(smtpPort));
        }
        final List<StandardUsernamePasswordCredentials> credentials =
                CredentialsMatchers.filter(
                        CredentialsProvider.lookupCredentials(
                                StandardUsernamePasswordCredentials.class,
                                Jenkins.get(),
                                ACL.SYSTEM,
                                domainRequirement),
                        CredentialsMatchers.withUsername(smtpUsername));
        for (final StandardUsernamePasswordCredentials cred : credentials) {
            if (StringUtils.equals(smtpPassword.getPlainText(), Secret.toString(cred.getPassword()))) {
                // If some credentials have the same username/password, use those.
                credentialsId = cred.getId();
                break;
            }
        }
        if (StringUtils.isBlank(credentialsId)) {
            // If we couldn't find any existing credentials,
            // create new credentials with the principal and secret and use it.
            final StandardUsernamePasswordCredentials newCredentials =
                    new UsernamePasswordCredentialsImpl(
                            CredentialsScope.GLOBAL,
                            null,
                            "Migrated from email-ext username/password",
                            smtpUsername,
                            smtpPassword.getPlainText());
            SystemCredentialsProvider.getInstance().getCredentials().add(newCredentials);
            credentialsId = newCredentials.getId();
        }

        smtpUsername = null;
        smtpPassword = null;
    }
}
