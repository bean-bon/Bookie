from requests import post
from flask import Flask, render_template, request
from flask_cors import CORS
from jproperties import Properties


properties = Properties()
with open("bookie.properties", 'rb') as config:
    properties.load(config)


app = Flask(__name__, static_folder='static')
CORS(app, resources={r"/*": {"origins": properties.get("CODE_RUNNER_URL").data}})


# Contents page
@app.route("/")
def contents():
    return render_template("index.html")


# API endpoint intended for communicating with my other project RemoteCodeRunner.
@app.route("/code_runner", methods=["POST"])
def code_runner():
    post_req = post(properties.get("CODE_RUNNER_URL").data, data=request.form)
    return post_req.json()


# Auto-generated chapter information
%%generatedRoutes


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)