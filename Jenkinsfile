// this uses the buildPlugin step from
// The jenkins-infra/pipeline-library repo

def recentLTS = "2.164.1"
def configurations = [
    [ platform: "linux", jdk: "8", jenkins: null ],
    [ platform: "windowspacker", jdk: "8", jenkins: recentLTS, javaLevel: "8" ],
]

buildPlugin(
    failFast: false,
    configurations: configurations
)
