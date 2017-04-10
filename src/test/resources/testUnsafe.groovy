def foo = [1, 2, 3, 4, 5]

jenkins.model.Jenkins.instance.toComputer()

"foo[3] = ${foo[3]}"