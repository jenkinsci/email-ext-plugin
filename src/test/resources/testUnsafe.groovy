def foo = [1, 2, 3, 4, 5]

jenkins.model.Jenkins.instance.createProject(hudson.model.FreeStyleProject, 'should-not-exist')

"foo[3] = ${foo[3]}"