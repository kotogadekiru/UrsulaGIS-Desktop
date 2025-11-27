@echo off
echo Starting UrsulaGIS Desktop Application (Debug Mode)...
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

REM Run the application with debug profile
echo Starting application in debug mode...
echo Debug port: 5005
echo Connect your IDE debugger to localhost:5005
echo.
call mvn javafx:run -Djavafx.options="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -Xmx2g -XX:+UseG1GC"

if %ERRORLEVEL% neq 0 (
    echo.
    echo Application exited with error code: %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Application closed successfully.
pause
