venv\bin\activate
gunicorn --bind "0.0.0.0:5000" app:app -w 4