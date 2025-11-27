@echo off
echo Running UrsulaGIS Preloader Tests...
echo.

echo Compiling and running unit tests...
mvn test -Dtest=UrsulaGISPreloaderUnitTest

echo.
echo Compiling and running basic tests...
mvn test -Dtest=UrsulaGISPreloaderTest

echo.
echo Skipping integration tests due to JavaFX toolkit requirements
echo Integration tests require full JavaFX environment setup

echo.
echo All working tests completed!
pause
