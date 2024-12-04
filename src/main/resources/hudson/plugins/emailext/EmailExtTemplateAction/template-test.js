function onSubmit() {
    var templateFile = document.getElementById('template_file_name').value;
    var buildId = document.getElementById('template_build').value;
    templateTester.renderTemplate(templateFile,buildId, function(t) {
        document.getElementById('rendered_template').src = "data:text/html;charset=utf-8," + escape(t.responseObject()[0]);
        var consoleOutput = t.responseObject()[1];
        if(consoleOutput.length == 0) {
            document.getElementById('output').style.display = 'none';
        } else {
            document.getElementById('output').style.display = 'block';
            document.getElementById('console_output').innerHTML = consoleOutput;
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelector(".test-template-form").addEventListener("submit", (event) => {
        event.preventDefault();
        
        onSubmit();
    });
});
