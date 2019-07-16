package hudson.plugins.emailext;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import static hudson.Util.nullify;

public class MailAccount extends AbstractDescribableImpl<MailAccount>{
    private String address = null;
    private String smtpHost = null;
    private String smtpPort = null;
    private String smtpUsername = null;
    private Secret smtpPassword = null;
    private boolean useSsl = false;
    private String advProperties = null;

    @DataBoundConstructor
    public MailAccount(JSONObject jo){
        address = nullify(jo.optString("address", null));
        smtpHost = nullify(jo.optString("smtpHost", null));
        smtpPort = nullify(jo.optString("smtpPort", null));
        if(jo.optBoolean("auth", false)){
            smtpUsername = nullify(jo.optString("smtpUsername", null));
            String pass = nullify(jo.optString("smtpPassword", null));
            if(pass != null) {
                smtpPassword = Secret.fromString(pass);
            }
        }
        useSsl = jo.optBoolean("useSsl", false);
        advProperties = nullify(jo.optString("advProperties", null));
    }

    public MailAccount(){

    }

    public boolean isValid() {
        return StringUtils.isNotBlank(address) && StringUtils.isNotBlank(smtpHost) && (!isAuth() || (StringUtils.isNotBlank(smtpUsername) && smtpPassword != null));
    }

    @Extension
    public static class MailAccountDescriptor extends Descriptor<MailAccount>{
        @Override
        public String getDisplayName(){
            return "";
        }
    }

    public boolean isAuth(){
        return smtpUsername != null;
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

    public Secret getSmtpPassword(){
        return smtpPassword;
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
        this.smtpPassword = Secret.fromString(smtpPassword);
    }

    public void setSmtpPassword(Secret smtpPassword){
        this.smtpPassword = smtpPassword;
    }

    public void setUseSsl(boolean useSsl){
        this.useSsl = useSsl;
    }

    public void setAdvProperties(String advProperties){
        this.advProperties = advProperties;
    }
}
