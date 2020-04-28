// this uses the buildPlugin step from
// The jenkins-infra/pipeline-library repo

/**
 * Get recommended configurations for testing.
 * Includes testing Java 8 and 11 on the newest LTS.
 */
static List<Map<String, String>> recommendedConfigurations() {
    def recentLTS = "2.222.3"
    def configurations = [
        [ platform: "s390x", jdk: "8", jenkins: null ],
        [ platform: "s390x", jdk: "11", jenkins: recentLTS, javaLevel: "8" ],
    ]
    return configurations
}

buildPlugin(
    failFast: false,
    useAci: false,
    configurations: recommendedConfigurations()
)
