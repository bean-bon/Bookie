// Template to be inserted alongside other dynamic Javascript.
// The code here will only work if run in a web server.

ace.require("ace/ext/language_tools");

function replaceAceBlockContents(filePath, editor) {
    fetch(filePath)
        .then(response => {
            if (!response.ok) {
                return "Unable to read file, please reload the page or undo to reset this block."
            }
            return response.text();
        })
        .then(fileContent => {
            editor.setValue(fileContent);
            editor.gotoLine(0);
        })
}

// This can only work if running in a web server with a "/code_runner" POST route.
// The returned JSON should be: {output: str, exit_code: str}.
function runCode(language, code_content, file_name, output_element_id, run_result_element_id) {
    const formData = new FormData();
    formData.append('code', code_content);
    formData.append('lang', language);
    formData.append('name', file_name);
    const callOptions = {
        method: 'POST',
        body: formData
    };
    fetch("/code_runner", callOptions)
        .then(response => {
            return response.json()
        })
        .then(json => {
            const output = json.output;
            const exit_code = json.exit_code;
            const output_element = document.getElementById(output_element_id);
            const run_result_element = document.getElementById(run_result_element_id);
            output_element.textContent = output;
            run_result_element.textContent = exit_code;
        })
        .catch(_ => {
            const outputElement = document.getElementById(output_element_id)
            outputElement.textContent = "Encountered an error while trying to decode server response, there may be a connection issue."
        });
}

function toggleContentsVisibility(toggleButton, contentsListID) {
    const contentsList = document.getElementById(contentsListID);
    if (contentsList.style.display === "none") {
        toggleButton.classes.add('open')
        toggleButton.classes.remove('closed')
        contentsList.style.display = "block"
    } else {
        toggleButton.classes.add('closed')
        toggleButton.classes.remove('open')
        contentsList.style.display = "none"
    }
}