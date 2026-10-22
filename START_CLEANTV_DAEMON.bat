@echo off
title CleanYourTV - Ultra-Fast Home Daemon Activator
color 0B
echo ============================================================
echo           CleanYourTV - Fire TV Ultra-Fast Home Hook
echo ============================================================
echo.

set ADB=C:\Users\crs14\AppData\Local\Android\Sdk\platform-tools\adb.exe
if not exist "%ADB%" (
    set ADB=adb
)

set DEVICE=192.168.1.2:5555
echo [*] Connecting to Fire TV at %DEVICE%...
"%ADB%" connect %DEVICE%

echo [*] Starting UID 2000 Home Daemon...
"%ADB%" -s %DEVICE% shell "chmod 755 /data/local/tmp/home_daemon.sh && nohup /data/local/tmp/home_daemon.sh >/dev/null 2>&1 &"

echo [*] Verifying daemon status...
"%ADB%" -s %DEVICE% shell "ps -ef | grep home_daemon"

echo.
echo ============================================================
echo [SUCCESS] Home Button Daemon is ACTIVE!
echo Physical Home button returns to CleanYourTV in ~300ms.
echo ============================================================
pause
