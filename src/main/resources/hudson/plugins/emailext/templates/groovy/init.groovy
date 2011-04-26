// just a few helper methods

/**
 * Checks if a plugin is installed based on it's short name.
 *
 * @param shortName the short name of the plugin to check for
 * @returns true if the plugin is installed, false otherwise.
 */
boolean isPluginInstalled(String shortName) {
	boolean result = false;
	def instance = hudson.model.Hudson.instance;
	if(instance != null) {
		result = instance.getPlugin(shortName) != null;
	}
	return result;
}
