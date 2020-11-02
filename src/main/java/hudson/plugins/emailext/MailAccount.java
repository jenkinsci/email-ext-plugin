package hudson.plugins.emailext;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.Util;

public class MailAccount extends AbstractDescribableImpl<MailAccount>{
    private String address;
    private String smtpHost;
    private String smtpPort = "25";
    private String smtpUsername;
    private Secret smtpPassword;
    private boolean useSsl;
    private String advProperties;
    private boolean defaultAccount;

    @Deprecated
    public MailAccount(JSONObject jo){
        address = Util.nullify(jo.optString("address", null));
        smtpHost = Util.nullify(jo.optString("smtpHost", null));
        smtpPort = Util.nullify(jo.optString("smtpPort", null));
        if(jo.optBoolean("auth", false)){
            smtpUsername = Util.nullify(jo.optString("smtpUsername", null));
            String pass = Util.nullify(jo.optString("smtpPassword", null));
            if(pass != null) {
                smtpPassword = Secret.fromString(pass);
            }
        }
        useSsl = jo.optBoolean("useSsl", false);
        advProperties = Util.nullify(jo.optString("advProperties", null));
    }

    @DataBoundConstructor
    public MailAccount() {

    }

    public boolean isValid() {
        return isFromAddressValid() && isSmtpServerValid() && isSmtpAuthValid();
    }

    public boolean isFromAddressValid() {
        return isDefaultAccount() || StringUtils.isNotBlank(address);
    }

    public boolean isSmtpServerValid() {
        return StringUtils.isNotBlank(smtpHost);
    }

    public boolean isSmtpAuthValid() {
        return StringUtils.isBlank(smtpUsername)
                || (StringUtils.isNotBlank(smtpUsername) && smtpPassword != null);
    }

    public boolean isDefaultAccount() {
        return defaultAccount;
    }

    void setDefaultAccount(boolean defaultAccount) {
        this.defaultAccount = defaultAccount;
    }

    @Extension
    public static class MailAccountDescriptor extends Descriptor<MailAccount>{
        @Override
        public String getDisplayName(){
            return "";
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

    public String getSmtpUsername(){
        return smtpUsername;
    }

    @DataBoundSetter
    public void setSmtpUsername(String smtpUsername){
        this.smtpUsername = Util.fixEmptyAndTrim(smtpUsername);
    }

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

    public boolean isUseSsl(){
        return useSsl;
    }

    @DataBoundSetter
    public void setUseSsl(boolean useSsl){
        this.useSsl = useSsl;
    }

    public String getAdvProperties(){
        return advProperties;
    }

    @DataBoundSetter
    public void setAdvProperties(String advProperties){
        this.advProperties = advProperties;
    }
}
