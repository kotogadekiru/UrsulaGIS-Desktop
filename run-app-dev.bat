@echo off
echo Starting UrsulaGIS Desktop Application (Development Mode)...
echo.

REM Clean and compile the project
echo Cleaning and compiling project...
call mvn clean compile -q

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo.
echo Compilation successful!
echo.

REM Run the application with development profile
echo Starting application in development mode...
call mvn javafx:run -Djavafx.options="-Dprism.verbose=true -Xmx2g -XX:+UseG1GC"

if %ERRORLEVEL% neq 0 (
    echo.
    echo Application exited with error code: %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Application closed successfully.
pause
