package hudson.plugins.emailext;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import static hudson.Util.nullify;

public class MailAccount extends AbstractDescribableImpl<MailAccount>{
    private String address;
    private String smtpHost;
    private String smtpPort;
    private String smtpUsername;
    private Secret smtpPassword;
    private boolean useSsl;
    private String advProperties;

    @DataBoundConstructor
    public MailAccount(JSONObject jo){
        address = nullify(jo.optString("address", null));
        smtpHost = nullify(jo.optString("smtpHost", null));
        smtpPort = nullify(jo.optString("smtpPort", null));
        smtpUsername = nullify(jo.optString("smtpUsername", null));
        smtpPassword = Secret.fromString(nullify(jo.optString("smtpPassword", null)));
        useSsl = jo.optBoolean("useSsl", false);
        advProperties = nullify(jo.optString("advProperties", null));
    }

    public MailAccount(){

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

    public String getSmtpHost(){
        return smtpHost;
    }

    public String getSmtpPort(){
        return smtpPort;
    }

    public String getSmtpUsername(){
        return smtpUsername;
    }

    public String getSmtpPassword(){
        return Secret.toString(smtpPassword);
    }

    public boolean isUseSsl(){
        return useSsl;
    }

    public String getAdvProperties(){
        return advProperties;
    }

    public void setAddress(String address){
        this.address = address;
    }

    public void setSmtpHost(String smtpHost){
        this.smtpHost = smtpHost;
    }

    public void setSmtpPort(String smtpPort){
        this.smtpPort = smtpPort;
    }

    public void setSmtpUsername(String smtpUsername){
        this.smtpUsername = smtpUsername;
    }

    public void setSmtpPassword(String smtpPassword){
        this.smtpPassword = Secret.fromString(smtpPassword);;
    }

    public void setUseSsl(boolean useSsl){
        this.useSsl = useSsl;
    }

    public void setAdvProperties(String advProperties){
        this.advProperties = advProperties;
    }
}
