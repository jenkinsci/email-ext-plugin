function onSubmit() {
    var templateFile = document.getElementById('template_file_name').value;
    var buildId = document.getElementById('template_build').value;
    
    var formData = new FormData();
    formData.append('templateFile', templateFile);
    formData.append('buildId', buildId);
    
    fetch('renderTemplate', {
        method: 'POST',
        body: formData
    })
    .then(function(response) {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(function(data) {
        document.getElementById('rendered_template').src = "data:text/html;charset=utf-8," + encodeURIComponent(data.renderedContent);
        var consoleOutput = data.consoleOutput;
        if(consoleOutput.length === 0) {
            document.getElementById('output').style.display = 'none';
        } else {
            document.getElementById('output').style.display = 'block';
            document.getElementById('console_output').textContent = consoleOutput;
        }
    })
    .catch(function(error) {
        console.error('Error rendering template:', error);
        document.getElementById('console_output').textContent = 'Error: ' + error.message;
        document.getElementById('output').style.display = 'block';
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelector(".test-template-form").addEventListener("submit", (event) => {
        event.preventDefault();
        
        onSubmit();
    });
});
