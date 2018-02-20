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
    private String smtp_host;
    private String smtp_port;
    private String smtp_username;
    private Secret smtp_password;
    private boolean use_ssl;
    private String adv_properties;

    @DataBoundConstructor
    public MailAccount(JSONObject jo){
        address = nullify(jo.optString("address", null));
        smtp_host = nullify(jo.optString("smtp_host", null));
        smtp_port = nullify(jo.optString("smtp_port", null));
        smtp_username = nullify(jo.optString("smtp_username", null));
        smtp_password = Secret.fromString(nullify(jo.optString("smtp_password", null)));
        use_ssl = jo.optBoolean("use_ssl", false);
        adv_properties = nullify(jo.optString("adv_properties", null));
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

    public String getSmtp_host(){
        return smtp_host;
    }

    public String getSmtp_port(){
        return smtp_port;
    }

    public String getSmtp_username(){
        return smtp_username;
    }

    public String getSmtp_password(){
        return Secret.toString(smtp_password);
    }

    public boolean isUse_ssl(){
        return use_ssl;
    }

    public String getAdv_properties(){
        return adv_properties;
    }

    public void setAddress(String address){
        this.address = address;
    }

    public void setSmtp_host(String smtp_host){
        this.smtp_host = smtp_host;
    }

    public void setSmtp_port(String smtp_port){
        this.smtp_port = smtp_port;
    }

    public void setSmtp_username(String smtp_username){
        this.smtp_username = smtp_username;
    }

    public void setSmtp_password(String smtp_password){
        this.smtp_password = Secret.fromString(smtp_password);;
    }

    public void setUse_ssl(boolean use_ssl){
        this.use_ssl = use_ssl;
    }

    public void setAdv_properties(String adv_properties){
        this.adv_properties = adv_properties;
    }
}
