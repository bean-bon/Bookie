echo Creating Python virtual environment...
pip install virtualenv >NUL
virtualenv venv >NUL
venv\Scripts\activate >NUL

echo Installing Python requirements
pip install -r requirements.txt >NUL

echo Installation complete!
pause