@echo off
echo WiFi 보안 모니터링 서비스 설치를 시작합니다...

REM Java가 설치된 경로 확인
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Java가 설치되어 있지 않습니다. Java를 설치해주세요.
    pause
    exit /b 1
)

REM 서비스 설치
sc create WifiSecurityService binPath= "\"%JAVA_HOME%\bin\java.exe\" -jar \"%~dp0target\wifi-security-checker-1.0-SNAPSHOT.jar\"" start= auto
if %ERRORLEVEL% neq 0 (
    echo 서비스 설치에 실패했습니다.
    pause
    exit /b 1
)

REM 서비스 시작
sc start WifiSecurityService
if %ERRORLEVEL% neq 0 (
    echo 서비스 시작에 실패했습니다.
    pause
    exit /b 1
)

echo WiFi 보안 모니터링 서비스가 성공적으로 설치되었습니다.
echo 서비스를 중지하려면 'sc stop WifiSecurityService'를 실행하세요.
echo 서비스를 제거하려면 'sc delete WifiSecurityService'를 실행하세요.
pause 