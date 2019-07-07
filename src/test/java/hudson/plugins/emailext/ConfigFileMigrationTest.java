package hudson.plugins.emailext;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class ConfigFileMigrationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @LocalData
    public void testMigrateOldData() {

        for (ConfigProvider cp : ConfigProvider.all()) {
            // as all the config files have been moved to global config,
            // all providers must not hold any files any more
            AbstractConfigProviderImpl acp = (AbstractConfigProviderImpl) cp;
            Assert.assertTrue("configs for " + acp.getProviderId() + " should be empty", acp.getConfigs().isEmpty());
        }

        Assert.assertEquals(1, getProvider(JellyTemplateConfig.JellyTemplateConfigProvider.class).getAllConfigs().size());
        Assert.assertEquals(1, GlobalConfigFiles.get().getConfigs().size());
    }

    private <T> T getProvider(Class<T> providerClass) {
        return j.getInstance().getExtensionList(providerClass).get(providerClass);
    }

}
