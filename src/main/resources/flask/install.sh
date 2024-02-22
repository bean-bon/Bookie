#!/bin/bash

exec >/dev/null

echo "Creating Python virtual environment..."
pip install virtualenv
virtualenv venv
source venv/bin/activate

echo "Installing Python requirements"
pip install -r requirements.txt

echo "Installation complete!"
read -r -p "Press [Enter] key to continue..."