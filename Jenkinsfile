/*
 * See the documentation for more options:
 * https://github.com/jenkins-infra/pipeline-library/
 */
buildPlugin(useContainerAgent: true, configurations: [
  // Test the common case (i.e., a recent LTS release).
  [ platform: 'linux', jdk: '8' ],

  // Test the bleeding edge of the compatibility spectrum (i.e., the latest supported Java runtime).
  [ platform: 'linux', jdk: '11' ],
])
