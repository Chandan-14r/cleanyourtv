$ErrorActionPreference = 'SilentlyContinue'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb)) { $adb = 'adb' }
$serial = '192.168.1.2:5555'
$daemon = Join-Path $PSScriptRoot 'home_daemon.sh'
$logDir = Join-Path $env:LOCALAPPDATA 'CleanYourTV'
$logFile = Join-Path $logDir 'watchdog.log'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
function Write-WatchdogLog([string]$message) {
    Add-Content -LiteralPath $logFile -Value "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $message"
}
Write-WatchdogLog 'Watchdog process started.'
$mutex = [Threading.Mutex]::new($false, 'Local\CleanYourTVFireTVWatchdog')
$ownsMutex = $false
try { $ownsMutex = $mutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $ownsMutex = $true }
if (-not $ownsMutex) { Write-WatchdogLog 'Another watchdog instance is already active.'; exit 0 }
$lastState = ''
try {
    while ($true) {
        if (Test-Path -LiteralPath $daemon) {
            & $adb connect $serial *> $null
            $state = (& $adb -s $serial get-state 2>$null | Out-String).Trim()
            if ($state -ne $lastState) {
                Write-WatchdogLog "Fire TV ADB state: $state"
                $lastState = $state
            }
            if ($state -eq 'device') {
                $probe = 'if [ -f /data/local/tmp/home_daemon.pid ]; then p=$(cat /data/local/tmp/home_daemon.pid); kill -0 "$p" 2>/dev/null; else exit 1; fi'
                & $adb -s $serial shell $probe *> $null
                if ($LASTEXITCODE -ne 0) {
                    & $adb -s $serial push $daemon /data/local/tmp/home_daemon.sh *> $null
                    if ($LASTEXITCODE -eq 0) {
                        $start = 'chmod 755 /data/local/tmp/home_daemon.sh && nohup /data/local/tmp/home_daemon.sh >/dev/null 2>&1 </dev/null &'
                        & $adb -s $serial shell $start *> $null
                        Write-WatchdogLog 'Started the Fire TV Home recovery daemon.'
                    } else {
                        Write-WatchdogLog 'Could not copy the daemon to the Fire TV.'
                    }
                }
            }
        } else {
            Write-WatchdogLog 'Local home_daemon.sh is missing.'
            break
        }
        Start-Sleep -Seconds 5
    }
} finally {
    if ($ownsMutex) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}