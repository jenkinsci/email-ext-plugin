class DoNotRunConstructor {
    static void main(String[] args) {}
    DoNotRunConstructor() {
      assert jenkins.model.Jenkins.instance.createProject(hudson.model.FreeStyleProject, 'should-not-exist')
    }
}
