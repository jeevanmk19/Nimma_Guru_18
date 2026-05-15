# Nimma Guru — Log Viewer Script
# Filters out system noise and shows only relevant app logs
# Also writes logs to nimmaguru-logs.txt in project root

Write-Host "📜 Nimma Guru — App Logs" -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop viewing logs" -ForegroundColor Gray
Write-Host ""

# Add ADB to PATH temporarily for this session
$adbPath = "C:\Android\Sdk\platform-tools"
if (Test-Path $adbPath) {
    $env:Path += ";$adbPath"
    Write-Host "✅ Added ADB to PATH: $adbPath" -ForegroundColor Green
    Write-Host ""
}

# Check if ADB is available
try {
    $adbCheck = Get-Command adb -ErrorAction Stop
}
catch {
    Write-Host "❌ ADB not found in PATH!" -ForegroundColor Red
    Write-Host "   Please add Android SDK platform-tools to your system PATH." -ForegroundColor Gray
    Write-Host "   See ADD-ADB-TO-PATH.md for step-by-step instructions." -ForegroundColor Gray
    exit 1
}

# Find the app PID automatically (renamed from $pid to avoid conflict with reserved variable)
$appPid = adb shell pidof com.nimmaguru.app

if ([string]::IsNullOrWhiteSpace($appPid)) {
    Write-Host "❌ App not running! Please start the app first." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Found app PID: $appPid" -ForegroundColor Green

# Create log file path
$logFile = Join-Path $PSScriptRoot "nimmaguru-logs.log"
Write-Host "📝 Logs also writing to: $logFile" -ForegroundColor Cyan
Write-Host "----------------------------------------"
Write-Host ""
Write-Host "💡 Now click the SUBMIT button on the Post Appreciation form!" -ForegroundColor Yellow
Write-Host ""

# Run logcat filtered to the app PID and tee to both console and file
adb logcat -v time --pid=$appPid | Tee-Object -FilePath $logFile
