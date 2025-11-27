#!/bin/bash

echo "Starting UrsulaGIS Desktop Application..."
echo

# Clean and compile the project
echo "Cleaning and compiling project..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo
    echo "ERROR: Compilation failed!"
    exit 1
fi

echo
echo "Compilation successful!"
echo

# Run the application
echo "Starting application..."
mvn javafx:run

if [ $? -ne 0 ]; then
    echo
    echo "Application exited with error code: $?"
    exit $?
fi

echo
echo "Application closed successfully."
