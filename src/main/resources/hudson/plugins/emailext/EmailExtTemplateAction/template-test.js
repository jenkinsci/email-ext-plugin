function onSubmit() {
    var templateFile = document.getElementById('template_file_name').value;
    var buildId = document.getElementById('template_build').value;
    
    var formData = new FormData();
    formData.append('templateFile', templateFile);
    formData.append('buildId', buildId);
    
    var rootURL = document.body.getAttribute('data-root-url') || '';
    var projectUrl = document.body.getAttribute('data-project-url') || '';
    var renderUrl = rootURL + projectUrl + 'templateTest/renderTemplate';
    
    fetch(renderUrl, {
        method: 'POST',
        body: formData
    })
    .then(function(response) {
        if (!response.ok) {
            return response.text().then(function(text) {
                throw new Error('HTTP ' + response.status + ': ' + (text || response.statusText));
            });
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
        var errorMsg = error.message || 'Unknown error';
        document.getElementById('console_output').textContent = 'Error: ' + errorMsg;
        document.getElementById('output').style.display = 'block';
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelector(".test-template-form").addEventListener("submit", (event) => {
        event.preventDefault();
        
        onSubmit();
    });
});
