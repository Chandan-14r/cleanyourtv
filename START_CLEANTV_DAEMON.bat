@echo off
setlocal
 title CleanYourTV - Home Recovery Daemon
 color 0B
 set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
 if not exist "%ADB%" set "ADB=adb"
 if not exist "%~dp0home_daemon.sh" (
   echo [ERROR] Missing daemon script: "%~dp0home_daemon.sh"
   pause
   exit /b 1
 )
 echo Enter the Fire TV's ADB address, including port 5555.
 set "DEVICE=192.168.1.2:5555"
 set /p "DEVICE=Device [%DEVICE%]: "
 if not defined DEVICE set "DEVICE=192.168.1.2:5555"
 echo.
 echo [*] Connecting to %DEVICE%...
 "%ADB%" connect %DEVICE%
 if errorlevel 1 goto adb_error
 "%ADB%" -s %DEVICE% get-state 2>nul | findstr /x "device" >nul
 if errorlevel 1 (
   echo [ERROR] Fire TV is not online. Enable ADB debugging and authorize this PC first.
   pause
   exit /b 1
 )
 echo [*] Installing daemon script...
 "%ADB%" -s %DEVICE% push "%~dp0home_daemon.sh" /data/local/tmp/home_daemon.sh
 if errorlevel 1 goto adb_error
 echo [*] Restarting daemon...
 "%ADB%" -s %DEVICE% shell "if [ -f /data/local/tmp/home_daemon.pid ]; then kill $(cat /data/local/tmp/home_daemon.pid) 2>/dev/null; fi; chmod 755 /data/local/tmp/home_daemon.sh && nohup /data/local/tmp/home_daemon.sh >/dev/null 2>&1 </dev/null &"
 if errorlevel 1 goto adb_error
 timeout /t 2 /nobreak >nul
 "%ADB%" -s %DEVICE% shell "ps -A | grep '[h]ome_daemon.sh'"
 if errorlevel 1 (
   echo [ERROR] Daemon did not appear to start. Check Fire TV logcat and ADB authorization.
   pause
   exit /b 1
 )
 echo.
 echo [SUCCESS] Daemon is running until the Fire TV reboots.
 pause
 exit /b 0
 :adb_error
 echo [ERROR] ADB command failed. Confirm debugging is enabled and this PC is authorized.
 pause
 exit /b 1