// this uses the buildPlugin step from
// The jenkins-infra/pipeline-library repo

def configurations = buildPlugin.recommendedConfigurations()
configurations += [ platform: "linux", jdk: "8", jenkins: "2.222" ]

buildPlugin(
    useAci: true,
    failFast: false,
    configurations: configurations
)
