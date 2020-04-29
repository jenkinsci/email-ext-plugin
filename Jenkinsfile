// this uses the buildPlugin step from
// The jenkins-infra/pipeline-library repo

static List<Map<String, String>> recommendedConfigurations() {
    def recentLTS = "2.222.3"
    def configurations = [
        [ platform: "linux", jdk: "8", jenkins: null ],
        [ platform: "linux", jdk: "11", jenkins: null ],
        
        [ platform: "s390x", jdk: "8", jenkins: null ],
        [ platform: "s390x", jdk: "11", jenkins: null ],
        
        [ platform: "ppc64le", jdk: "8", jenkins: null ],
        [ platform: "ppc64le", jdk: "11", jenkins: null ],        
                
        [ platform: "arm64", jdk: "8", jenkins: null ],
        [ platform: "arm64", jdk: "11", jenkins: null ],
    ]
    return configurations
}

buildPlugin(
    failFast: false,
    configurations: recommendedConfigurations()
)
