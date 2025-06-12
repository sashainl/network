@echo off
echo Building WiFi Security Checker...

:: Build the project
call mvn clean package

if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

:: Create distribution directory
if not exist dist mkdir dist

:: Copy files to distribution directory
copy target\wifi-security-checker-1.0-SNAPSHOT-jar-with-dependencies.jar dist\wifi-security-checker.jar
copy install-service.bat dist\
copy README.md dist\
copy LICENSE dist\

:: Create launcher batch file
echo @echo off > dist\run-wifi-checker.bat
echo java -jar wifi-security-checker.jar >> dist\run-wifi-checker.bat

:: Create ZIP package
powershell Compress-Archive -Path dist\* -DestinationPath wifi-security-checker.zip -Force

echo.
echo Build completed successfully!
echo Distribution package created: wifi-security-checker.zip
echo.
echo To run the application:
echo 1. Extract wifi-security-checker.zip
echo 2. Double-click run-wifi-checker.bat
echo.
echo To install as a Windows service:
echo 1. Extract wifi-security-checker.zip
echo 2. Run install-service.bat as administrator 