package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

@WithJenkins
class ConfigFileMigrationTest {

    @Test
    @LocalData
    void testMigrateOldData(JenkinsRule j) {

        for (ConfigProvider cp : ConfigProvider.all()) {
            // as all the config files have been moved to global config,
            // all providers must not hold any files anymore
            AbstractConfigProviderImpl acp = (AbstractConfigProviderImpl) cp;
            assertTrue(acp.getConfigs().isEmpty(), "configs for " + acp.getProviderId() + " should be empty");
        }

        assertEquals(
                1,
                getProvider(j, JellyTemplateConfig.JellyTemplateConfigProvider.class)
                        .getAllConfigs()
                        .size());
        assertEquals(1, GlobalConfigFiles.get().getConfigs().size());
    }

    private static <T> T getProvider(JenkinsRule j, Class<T> providerClass) {
        return j.getInstance().getExtensionList(providerClass).get(providerClass);
    }
}
